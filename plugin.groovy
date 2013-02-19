import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.ui.popup.JBPopupFactory

import static intellijeval.PluginUtil.registerAction
import static intellijeval.PluginUtil.show

def thisPluginPath = pluginPath
registerAction("WordCloud", "ctrl alt shift T") { AnActionEvent event ->
	JBPopupFactory.instance.createActionGroupPopup(
			"Show Word Cloud",
			new DefaultActionGroup().with {
				add(new AnAction("Plain text") {
					@Override void actionPerformed(AnActionEvent actionEvent) {
						def files = PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(actionEvent.dataContext).toList()
						WordCloud.showWordCloudFor(files, thisPluginPath)
					}
				})
//				add(new AnAction("Identifiers") {
//					@Override void actionPerformed(AnActionEvent e) { WordCloud.showIdentifiersCloud(e.dataContext, thisPluginPath) }
//				})
				add(new AnAction("Open browser") {
					@Override void actionPerformed(AnActionEvent e) { WordCloud.openCurrentEditorBrowser() }
				})
				it
			},
			event.dataContext,
			JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
			true
	).showCenteredInCurrentWindow(event.project)
}
show("reloaded")

