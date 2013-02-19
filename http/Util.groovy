package http
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.PerformInBackgroundOption
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import org.jetbrains.annotations.Nullable

import java.util.concurrent.atomic.AtomicReference

class Util {
	static def startHttpServerIfNotRunning(String id, String pluginPath, Closure handler, Closure errorHandler) {
		changeGlobalVar(id) { previousServer ->
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
		changeGlobalVar(id) { previousServer ->
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

	static doInBackground(String taskDescription = "", boolean canBeCancelled = true,
	                      PerformInBackgroundOption backgroundOption = PerformInBackgroundOption.ALWAYS_BACKGROUND,
	                      Closure task, Closure whenCancelled = {}, Closure whenDone) {
		AtomicReference result = new AtomicReference(null)
		new Task.Backgroundable(null, taskDescription, canBeCancelled, backgroundOption) {
			@Override void run(ProgressIndicator indicator) { result.set(task.call(indicator)) }
			@Override void onSuccess() { whenDone.call(result.get()) }
			@Override void onCancel() { whenCancelled.call() }

		}.queue()
	}

	@Nullable static <T> T changeGlobalVar(String varName, @Nullable initialValue = null, Closure callback) {
		def actionManager = ActionManager.instance
		def action = actionManager.getAction(asActionId(varName))

		def prevValue = (action == null ? initialValue : action.value)
		T newValue = (T) callback.call(prevValue)

		// unregister action only after callback has been invoked in case it crashes
		if (action != null) actionManager.unregisterAction(asActionId(varName))

		// anonymous class below will keep reference to outer object but that should be ok
		// because its class is not a part of reloadable plugin
		actionManager.registerAction(asActionId(varName), new AnAction() {
			final def value = newValue
			@Override void actionPerformed(AnActionEvent e) {}
		})

		newValue
	}

	@Nullable static <T> T setGlobalVar(String varName, @Nullable varValue) {
		changeGlobalVar(varName){ varValue }
	}

	@Nullable static <T> T getGlobalVar(String varName) {
		def action = ActionManager.instance.getAction(asActionId(varName))
		action == null ? null : action.value
	}

	private static asActionId(String globalVarKey) {
		"IntelliJEval-" + globalVarKey
	}
}

