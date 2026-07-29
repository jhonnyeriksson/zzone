# -*- mode: python; python-indent: 4 -*-
import ncs
from ncs.application import Service


class ServiceCallbacks(Service):

    # Logical CFS hostname -> real NSO device name
    DEVICE_MAP = {
        "router1": "XRv1",
        "router2": "XRv2",
    }

    @Service.create
    def cb_create(self, tctx, root, service, proplist):
        logical_hostname = service.hostname
        self.log.info('CFS create(service={}, hostname={})'.format(
            service.name, logical_hostname))

        if logical_hostname not in self.DEVICE_MAP:
            raise Exception(
                "Unknown hostname '{}'; expected one of: {}".format(
                    logical_hostname, ", ".join(sorted(self.DEVICE_MAP))
                )
            )

        device_name = self.DEVICE_MAP[logical_hostname]
        self.log.info('Mapped {} -> {}'.format(logical_hostname, device_name))

        if device_name not in root.devices.device:
            raise Exception("Mapped device {} does not exist in NSO".format(
                device_name))

        # Create stacked RFS instance with the real device name.
        rfs = root.RFS.create(service.name)
        rfs.hostname = device_name

        return proplist


class Main(ncs.application.Application):
    def setup(self):
        self.log.info('Main RUNNING')
        self.register_service('CFS-servicepoint', ServiceCallbacks)

    def teardown(self):
        self.log.info('Main FINISHED')
