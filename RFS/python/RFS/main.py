# -*- mode: python; python-indent: 4 -*-
import ncs
import ncs.template
from ncs.application import Service


class ServiceCallbacks(Service):

    # hostname -> [(loopback_id, loopback_ip), ...]
    LOOPBACK_MAP = {
        "XRv1": [
            (55, "10.0.0.1"),
            (56, "10.0.0.11"),
        ],
        "XRv2": [
            (55, "10.0.0.2"),
            (56, "10.0.0.12"),
        ],
    }

    @Service.create
    def cb_create(self, tctx, root, service, proplist):
        hostname = service.hostname
        self.log.info('RFS create(service={}, hostname={})'.format(
            service.name, hostname))

        if hostname not in self.LOOPBACK_MAP:
            raise Exception("No loopbacks defined for {}".format(hostname))

        device = root.devices.device[hostname]
        template = ncs.template.Template(service)

        for loopback_id, loopback_ip in self.LOOPBACK_MAP[hostname]:
            loopback_name = "Loopback{}".format(loopback_id)

            if loopback_id in device.config.interface.Loopback:
                raise Exception(
                    "{} already exists on {}; refusing to overwrite".format(
                        loopback_name, hostname
                    )
                )

            self.log.info("{} will be created with {}".format(
                loopback_name, loopback_ip))

            vars = ncs.template.Variables()
            vars.add("HOSTNAME", hostname)
            vars.add("LOOPBACK_ID", loopback_id)
            vars.add("LOOPBACK_IP", loopback_ip)
            template.apply("RFS-template", vars)

        return proplist


class Main(ncs.application.Application):
    def setup(self):
        self.log.info('Main RUNNING')
        self.register_service('RFS-servicepoint', ServiceCallbacks)

    def teardown(self):
        self.log.info('Main FINISHED')
