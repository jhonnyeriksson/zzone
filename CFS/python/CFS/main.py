# -*- mode: python; python-indent: 4 -*-
import ncs
from ncs.application import Service


class ServiceCallbacks(Service):

    @Service.create
    def cb_create(self, tctx, root, service, proplist):
        hostname = service.hostname
        self.log.info('CFS create(service={}, hostname={})'.format(
            service.name, hostname))

        # Create stacked RFS instance; RFS decides which loopbacks to push.
        rfs = root.RFS.create(service.name)
        rfs.hostname = hostname

        return proplist


class Main(ncs.application.Application):
    def setup(self):
        self.log.info('Main RUNNING')
        self.register_service('CFS-servicepoint', ServiceCallbacks)

    def teardown(self):
        self.log.info('Main FINISHED')
