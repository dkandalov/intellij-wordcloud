import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.fileEditor.FileEditorManagerAdapter
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.util.messages.MessageBusConnection
import http.Util

import static com.intellij.openapi.fileEditor.FileEditorManagerListener.FILE_EDITOR_MANAGER
import static intellijeval.PluginUtil.currentFileIn
import static intellijeval.PluginUtil.registerAction
import static intellijeval.PluginUtil.show

boolean trackCurrentFile = false
String thisPluginPath = pluginPath
registerAction("WordCloud", "ctrl alt shift T") { AnActionEvent event ->
	JBPopupFactory.instance.createActionGroupPopup(
			"Word Cloud",
			new DefaultActionGroup().with {
				add(new AnAction("Show word cloud for selected files/packages") {
					@Override void actionPerformed(AnActionEvent actionEvent) {
						def files = PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(actionEvent.dataContext).toList()
						WordCloud.showWordCloudFor(files, thisPluginPath)
					}
				})
				add(new Separator())
				add(new AnAction("Track opened file and build word cloud for it") {
					@Override void actionPerformed(AnActionEvent e) {
						// TODO
					}
				})
				add(new AnAction("Show word cloud for currently open file") {
					@Override void actionPerformed(AnActionEvent e) {
						WordCloud.openCurrentEditorBrowser()
					}
				})
				it
			},
			event.dataContext,
			JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
			true
	).showCenteredInCurrentWindow(event.project)
}
show("Loaded WordCloud action<br/>Use Ctrl+Alt+Shift+T to open popup window")


def project = event.project
Util.changeGlobalVar("editorTracker") { oldTracker ->
	oldTracker?.stop()
	if (trackCurrentFile)
		new EditorTracker(project, thisPluginPath).start()
}

class EditorTracker {
	Project project
	MessageBusConnection connection
	String thisPluginPath

	EditorTracker(Project project, String thisPluginPath) {
		this.project = project
		this.thisPluginPath = thisPluginPath
	}

	def start() {
		WordCloud.updateCurrentEditorWordCloud(currentFileIn(project), thisPluginPath)

		connection = project.messageBus.connect()
		connection.subscribe(FILE_EDITOR_MANAGER, new FileEditorManagerAdapter() {
			@Override void selectionChanged(FileEditorManagerEvent event) {
				if (event.newFile != null) {
					WordCloud.updateCurrentEditorWordCloud(event.newFile, thisPluginPath)
				}
			}
		})
		this
	}

	def stop() {
		connection.disconnect()
	}
}


