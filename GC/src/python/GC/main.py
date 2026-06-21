import ncs
import ncs.template


class ServiceCallbacks(ncs.application.Service):

    @ncs.application.Service.create
    def cb_create(self, tctx, root, service, proplist):

        self.log.info(f"Creating service {service.id}")

        vars = ncs.template.Variables()

        for pe in service.transport_information.options.settings.equipment_information.PE:

            if pe.node == "XRv1":
                loopback = "10.0.0.1"
            elif pe.node == "XRv2":
                loopback = "10.0.0.2"
            else:
                loopback = "0.0.0.0"

            vars.add("LOOPBACK", loopback)

        template = ncs.template.Template(service)
        template.apply("GC-template", vars)

        return proplist


class Main(ncs.application.Application):

    def setup(self):
        self.log.info("GC Python started")
        self.register_service("GC", ServiceCallbacks)

    def teardown(self):
        self.log.info("GC Python stopped")