import ncs

class Main(ncs.application.Application):

    def setup(self):
        self.log.info("Python package started")

    def teardown(self):
        self.log.info("Python package stopped")