# -*- mode: python; python-indent: 4 -*-
import ncs
import ncs.template
from ncs.application import Service


# ------------------------
# SERVICE CALLBACK EXAMPLE
# ------------------------
class ServiceCallbacks(Service):

    # The create() callback is invoked inside NCS FASTMAP and
    # must always exist.
    @Service.create
    def cb_create(self, tctx, root, service, proplist):

        self.log.info(f"MAKE-LOOPBACK for {service.hostname}")

        LOOPBACK_MAP = {
            "XRv1": "10.0.0.1",
            "XRv2": "10.0.0.2",
        }

        hostname = service.hostname

        if hostname not in LOOPBACK_MAP:
            raise Exception(f"No loopback defined for {hostname}")

        loopback_ip = LOOPBACK_MAP[hostname]

        loopback_id = 55
        loopback_name = f"Loopback{loopback_id}"

        device = root.devices.device[hostname]

        # -----------------------------
        # CHECK IF LOOPBACK EXISTS
        # -----------------------------
        exists = False

        try:
            if device.config.interface_configurations.interface_configuration.exists(loopback_name):
                exists = True
        except Exception:
            exists = False

        if exists:
            self.log.info(f"{loopback_name} already exists on {hostname} → reusing")
        else:
            self.log.info(f"{loopback_name} does NOT exist → will create")

        # -----------------------------
        # TEMPLATE VARIABLES
        # -----------------------------
        vars = ncs.template.Variables()
        vars.add("HOSTNAME", hostname)
        vars.add("LOOPBACK_ID", loopback_id)
        vars.add("LOOPBACK_IP", loopback_ip)

        template = ncs.template.Template(service)
        template.apply("MAKE-LOOPBACK-template", vars)

        return proplist() and post_modification() callbacks are optional,
    # and are invoked outside FASTMAP. pre_modification() is invoked before
    # create, update, or delete of the service, as indicated by the enum
    # ncs_service_operation op parameter. Conversely
    # post_modification() is invoked after create, update, or delete
    # of the service. These functions can be useful e.g. for
    # allocations that should be stored and existing also when the
    # service instance is removed.

    # @Service.pre_modification
    # def cb_pre_modification(self, tctx, op, kp, root, proplist):
    #     self.log.info('Service premod(service=', kp, ')')

    # @Service.post_modification
    # def cb_post_modification(self, tctx, op, kp, root, proplist):
    #     self.log.info('Service postmod(service=', kp, ')')





# ---------------------------------------------
# COMPONENT THREAD THAT WILL BE STARTED BY NCS.
# ---------------------------------------------
class Main(ncs.application.Application):
    def setup(self):
        # The application class sets up logging for us. It is accessible
        # through 'self.log' and is a ncs.log.Log instance.
        self.log.info('Main RUNNING')

        # Service callbacks require a registration for a 'service point',
        # as specified in the corresponding data model.
        #
        self.register_service('MAKE-LOOPBACK-servicepoint', ServiceCallbacks)

        # If we registered any callback(s) above, the Application class
        # took care of creating a daemon (related to the service/action point).

        # When this setup method is finished, all registrations are
        # considered done and the application is 'started'.

    def teardown(self):
        # When the application is finished (which would happen if NCS went
        # down, packages were reloaded or some error occurred) this teardown
        # method will be called.

        self.log.info('Main FINISHED')
