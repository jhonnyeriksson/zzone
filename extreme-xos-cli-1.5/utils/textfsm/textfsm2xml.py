#!/usr/local/bin/python2

import sys
import getopt
import textfsm
from dicttoxml import dicttoxml
from xml.dom import minidom

# This is a simple tool using the TextFSM tool to parse operational data
# from a network device and convert it into XML format accepted by Cisco NSO.
#
# The tool is used directly by the NSO CLI NEDs. Hence it must reside in the
# <NED name>/bin directory to be found.
#
# The tool can also be used standalone when protoyping new TextFSM parser templates.
# The following command options are suitable when prototyping.
#
# ./textfsm2xml.py --template=<path to template file> --file=<path to file containing device dump>


class Usage(Exception):
  """Error in command line execution."""

#
# Most pre made TextFSM parser templates are using
# leaf names etc in camel case format.
# This simple routine converts camel case names to
# lowercase with hyphen format, which is the NSO/NED
# standard format.
#
# thisIsAnExample -> this-is-an-example
# 
def decamelize(input):
  output = [input[0].lower()]
  previous = None
  capitals = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'
  skips = '_'
  for c in input[1:]:
    if c in skips:
      continue
    if c in capitals:
      if previous != None and previous not in capitals:
        output.append('-')
      output.append(c.lower())
    else:
      output.append(c)
    previous=c
    
  return str.join('', output)


#
# Simple routine that builds a standard dictionary of the arrays of
# parsed values together with the FSM header containing the corresponding
# leaf names.
#
def dictize(objects, fsm):
  objs = []
  for row in objects:
    dictionary = {}
    for i, e in enumerate(row):
      dictionary[decamelize(fsm.header[i])] = e
    objs.append(dictionary)
      
  return objs

#
# This utility contains quirks to transform the generated XML
# containing the TextFSM parsed data into a format that is
# accepted by NSO
#
def transform(xml):
  dom = minidom.parseString(xml)
  top = dom.childNodes[0]

  # List nodes are typically given in this format
  # <foo>
  #   <item>bar</item>
  #   <item>BAR</item>
  # </foo>
  # Needs to be transformed like this:
  # <foo>
  #   <item>bar</item>
  # </foo>
  # <foo>
  #   <item>BAR</item>
  # </foo>
  for child in top.childNodes:
    for item in child.getElementsByTagName('item'):
      parent = item.parentNode
      adoptive = dom.createElement(parent.nodeName)
      parent.parentNode.appendChild(adoptive)
      parent.removeChild(item)
      adoptive.appendChild(item)

  return dom.toprettyxml(indent="   ")
    

def main(argv=None):
  if argv is None:
    argv = sys.argv

  template=None
  dump=None

  try:
    opts, args = getopt.getopt(argv[1:], 'ht:f:d:', ['help','template=','file=','dump='])
  except getopt.error, msg:
    raise Usage(msg)

  for opt, arg in opts:
    if opt in ('-h', '--help'):
      print help_msg
      return 0
    elif opt in ('-t','--template'):
      template = open(arg)
    elif opt in ('-f','--file'):
      dump = open(arg).read()
    elif opt in ('-d','--dump'):
      dump = arg
    else:
      raise Usage('Bad argument: %s' % arg)
  
  if dump == None and template == None:
    raise Usage('%s : Invalid arguments.' % sys.argv[0])

  # Parse using TextFSM
  fsm = textfsm.TextFSM(template)
  objects = fsm.ParseText(dump)

  # Convert to XML
  xml = dicttoxml(dictize(objects, fsm), custom_root='top', attr_type=False)
  print transform(xml)
 
if __name__ == '__main__':
  help_msg = '%s [--help] --template=<template-file> --file=<cli-dump-file>\n' % sys.argv[0]
  try:
    sys.exit(main())
  except Usage, err:
    print >>sys.stderr, err
    print >>sys.stderr, 'For help use --help'
    sys.exit(2)

