import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.fileEditor.FileEditorManagerAdapter
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.util.messages.MessageBusConnection
import http.Util

import static com.intellij.openapi.fileEditor.FileEditorManagerListener.FILE_EDITOR_MANAGER
import static intellijeval.PluginUtil.*


boolean trackCurrentFile = false
String thisPluginPath = pluginPath

def wordCloudActions = new DefaultActionGroup("Word Cloud", true).with {
	add(new AnAction("Show cloud for selected file/package") {
		@Override void actionPerformed(AnActionEvent actionEvent) {
			def files = PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(actionEvent.dataContext).toList()
			WordCloud.showWordCloudFor(files, thisPluginPath)
		}
	})
	add(new DefaultActionGroup("Live Cloud", true).with {
		add(new AnAction() {
			@Override void actionPerformed(AnActionEvent event) {
				trackCurrentFile = !trackCurrentFile
				if (trackCurrentFile) {
					restartEditorTracker(event.project, thisPluginPath)
					show("Started")
				} else {
					stopEditorTracker()
					show("Stopped")
				}
			}

			@Override void update(AnActionEvent event) {
				event.presentation.text = (trackCurrentFile ? "Stop tracking open file" : "Start tracking open file")
			}
		})
		add(new AnAction("Show in browser") {
			@Override void actionPerformed(AnActionEvent event) {
				WordCloud.openCurrentEditorBrowser()
			}

			@Override void update(AnActionEvent event) {
				event.presentation.enabled = trackCurrentFile
			}
		})
		it
	})
	it
}
addToActionGroup("ToolsMenu", wordCloudActions)

registerAction("WordCloud", "ctrl alt shift T") { AnActionEvent event ->
	JBPopupFactory.instance.createActionGroupPopup(
			"Word Cloud",
			wordCloudActions,
			event.dataContext,
			JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
			true
	).showCenteredInCurrentWindow(event.project)
}
show("Loaded WordCloud action<br/>Use Ctrl+Alt+Shift+T to open popup window<br/>or Main Menu -> Tools -> Word Cloud")



def restartEditorTracker(Project project, String thisPluginPath) {
	Util.changeGlobalVar("editorTracker") { oldTracker ->
		oldTracker?.stop()
		new EditorTracker(project, thisPluginPath).start()
	}
}

def stopEditorTracker() {
	Util.changeGlobalVar("editorTracker") { oldTracker ->
		oldTracker?.stop()
	}
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

static def addToActionGroup(String actionGroupId, AnAction action) {
	def myId = ActionManager.instance.getId(action)
	def actionGroup = findActionGroup(actionGroupId)
	actionGroup.getChildren(null).findAll {
		myId == ActionManager.instance.getId(it)
	}.each {
		actionGroup.remove(it)
	}
	actionGroup.add(action)
}

private static DefaultActionGroup findActionGroup(String actionGroupId) {
	if (actionGroupId != null && actionGroupId) {
		def action = ActionManager.instance.getAction(actionGroupId)
		action instanceof DefaultActionGroup ? action : null
	} else {
		null
	}
}



