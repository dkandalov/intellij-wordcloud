package http

import static intellijeval.PluginUtil.getCachedBy

class Util {
	static def startHttpServer(String id, String pluginPath, Closure handler, Closure errorHandler) {
		getCachedBy(id) { previousServer ->
			if (previousServer != null) return previousServer

			def server = new SimpleHttpServer()
			def started = false
			for (port in (8100..10000)) {
				try {
					server.start(port, pluginPath, handler, errorHandler)
					started = true
					break
				} catch (BindException ignore) {
				}
			}
			if (!started) throw new IllegalStateException("Failed to start server '${id}'")
			server
		}
	}

	static SimpleHttpServer restartHttpServer(String id, String pluginPath, Closure handler, Closure errorHandler) {
		getCachedBy(id) { previousServer ->
			if (previousServer != null) {
				previousServer.stop()
			}

			def server = new SimpleHttpServer()
			def started = false
			for (port in (8100..10000)) {
				try {
					server.start(port, pluginPath, handler, errorHandler)
					started = true
					break
				} catch (BindException ignore) {
				}
			}
			if (!started) throw new IllegalStateException("Failed to start server '${id}'")
			server
		}
	}
}

