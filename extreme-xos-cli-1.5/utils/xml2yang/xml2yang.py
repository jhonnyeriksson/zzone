#!/usr/local/bin/python2
import sys
import getopt
from xml.dom import minidom

# This is a simple tool that creates a YANG model out of the
# information provided in a XML dump.
# The YANG model is created with a best effort approach, using
# the following assumptions:
# 1. A leaf is a node either without children or with children
#    that are text nodes.
# 2. A list is node that has one or many element nodes as children
#    and has one or more siblings with the same node name.
# 3. The key element in a list is the name of the first element node
#    child in the corresponding XML dump
#
# The XML dump used for building the YANG model must contain enough
# information to determine the assumptions described above.

class Usage(Exception):
  """Error in command line execution."""

# Format string for the live-status specific YANG annotations
annotation     = ["tailf:meta-data \"ned-livestats:parser-info\" {",
                  "  tailf:meta-value \"{'show':'%s','template':'%s'}\";",
                  "}"]

command = None
template = None

#
# Adds indentation
#
def pad (indent):
  return " "*indent

# Append the template YANG annotaion
def printAnnotation(indent):
    global annotation, template, command
    if template != None and command != None:
      indent +=2
      print pad(indent) + annotation[0]
      print pad(indent) + annotation[1] % (command,template)
      print pad(indent) + annotation[2]

#
# Prints YANG construct for a leaf
#
def printLeaf(name, indent, isTop):
  print pad(indent) + "leaf %s {" % name
  if isTop:
    printAnnotation(indent)
    print pad(indent) + "  config false;"
  print pad(indent) + "  type string;"
  print pad(indent) + "}"

#
# Prints YANG construct for a list
#
def printList(name, key, indent, isTop):
  print pad(indent) + "list %s {" % name
  if isTop:
    printAnnotation(indent)
    print pad(indent) + "  config false;"
  print pad(indent) + "  key %s;" % key
#
# Prints YANG construct for a container
#
def printContainer(name, indent, isTop):
  print pad(indent) + "container %s {" % name
  if isTop:
    printAnnotation(indent)
    print pad(indent) + "  config false;"

#
# Prints closing curly brace for
# list and container constructs 
#
def printEnd(indent):
  print pad(indent) + "}"

#
# Checks if a DOM node is a leaf
#
def isLeaf(node):
  if node.hasChildNodes:
    for c in node.childNodes:
      if c.nodeType == node.ELEMENT_NODE:
        return False
  return True

#
# Checks if a DOM node is a list
#
def isList(node):
  s = node.nextSibling
  while s != None:
    if s.nodeName == node.nodeName:
      return True
    s = s.nextSibling
  return False;


#
# Fetch the first child element node 
#
def firstChild(node):
  c = node.firstChild
  while c != None:
    if c.nodeType == node.ELEMENT_NODE:
      break
    c = nextUniqueSibling(c)
  return c

#
# Fetch next sibling which is an element node
# and has a name that differ from current
#
def nextUniqueSibling(node):
  s = node.nextSibling
  while s != None:
    if s.nodeType == s.ELEMENT_NODE and s.nodeName != node.nodeName:
      break
    s = s.nextSibling
  return s

#
# Build the YANG schema
#
def yangify(node, name=None, indent=0):
  if node == None:
    return;

  if name == None:
    name = node.nodeName;
    isTop = False
  else:
    isTop = True
    
  if isLeaf(node):
    printLeaf(name, indent, isTop)
  else:
    if isList(node):
      printList(name, firstChild(node).nodeName, indent, isTop)                                      
    else:                                
      printContainer(name, indent, isTop)
    yangify(node=firstChild(node), indent=indent+2)
    printEnd(indent)

  yangify(node=nextUniqueSibling(node), indent=indent)

  
def main(argv=None):
  global template, command
  if argv is None:
    argv = sys.argv

  top=None

  try:
    opts, args = getopt.getopt(argv[1:], 'hx:t:f:c:', ['help','xml=','top=','file=','command='])
  except getopt.error, msg:
    raise Usage(msg)

  for opt, arg in opts:
    if opt in ('-h', '--help'):
      print help_msg
      return 0
    elif opt in ('-t','--top'):
      top = arg
    elif opt in ('-x','--xml'):
      xml = open(arg).read()
    elif opt in ("-f", "--file"):
      template = arg
    elif opt in ("-c", "--command"):
      command = arg
    else:
      raise Usage('Bad argument: %s' % arg)
  
  if xml == None and top == None:
    raise Usage('%s : Invalid arguments.' % sys.argv[0])

  # Parse XML
  dom =  minidom.parseString(xml)

  yangify(firstChild(dom.documentElement), top)
  
 
if __name__ == '__main__':
  help_msg = '%s [--help] --file=<template-file> --command=<cli show command> --xml=<file withxml dump> [--top=<name of top node>]\n' % sys.argv[0]
  try:
    sys.exit(main())
  except Usage, err:
    print >>sys.stderr, err
    print >>sys.stderr, 'For help use --help'
    sys.exit(2)

