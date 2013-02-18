import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.PerformInBackgroundOption
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*

import javax.swing.*

import static WordCloud.FileProcessing.*
import static http.Util.restartHttpServer
import static intellijeval.PluginUtil.show
import static intellijeval.PluginUtil.showInConsole

/**
 * User: dima
 * Date: 18/11/2012
 */
class WordCloud {
	static def showTextCloud(DataContext dataContext, String pluginPath) {
		showCloud(new TextualWordOccurrences(), dataContext, pluginPath)
	}

	static def showIdentifiersCloud(DataContext dataContext, String pluginPath) {
		Project project = PlatformDataKeys.PROJECT.getData(dataContext)
		showCloud(new IdentifiersOccurrences(project), dataContext, pluginPath)
	}

	private static def showCloud(WordCloudSource wordCloudSource, DataContext dataContext, String pluginPath) {
		String wordsAsJSON = ""
		List<VirtualFile> files = PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext).toList()
		Project project = PlatformDataKeys.PROJECT.getData(dataContext)

		new Task.Backgroundable(project, "Preparing word cloud...", true, PerformInBackgroundOption.ALWAYS_BACKGROUND) {
			@Override void run(ProgressIndicator indicator) {
				ApplicationManager.application.runReadAction {
					try {
						Map wordOccurrences = wordCloudSource.wordOccurrencesFor(files, indicator)

						wordsAsJSON = convertToJSON(wordOccurrences)

						SwingUtilities.invokeLater{
							def output = wordOccurrences.entrySet().sort{it.value}.join("\n")
							showInConsole(output, project)
						}
					} catch (Exception e) {
						showInConsole(e, project)
					}
				}
			}

			@Override void onSuccess() {
				def handler = { ["/words.json": wordsAsJSON].get(it) }
				def server = restartHttpServer("WordCloud_HttpServer", pluginPath, handler, { it.printStackTrace() })
				BrowserUtil.launchBrowser("http://localhost:${server.port}/wordcloud.html")
			}
		}.queue()
	}

	static String convertToJSON(Map wordOccurrences) {
		if (wordOccurrences.isEmpty()) return '{"words": []}'

		def min = wordOccurrences.min { it.value }.value
		def max = wordOccurrences.max { it.value }.value
		def normalizedSizeOf = { entry ->
			def word = entry.key
			def size = entry.value
			if (word.size() < 5) size += (max * 0.3) // this is to make shorter words more noticeable

			Math.round((double) 5 + ((size - min) * 75 / (max - min)))
		}
		"""{"words": [
${wordOccurrences.entrySet().sort{ -it.value }.take(600).collect { '{"text": "' + it.key + '", "size": ' + normalizedSizeOf(it) + '}' }.join(",\n")}
]}
"""
	}

	private interface WordCloudSource {
		Map<String, Integer> wordOccurrencesFor(List<VirtualFile> files, ProgressIndicator indicator)
	}

	private static class IdentifiersOccurrences implements WordCloudSource {
		private final Project project

		IdentifiersOccurrences(Project project) {
			this.project = project
		}

		@Override Map<String, Integer> wordOccurrencesFor(List<VirtualFile> files, ProgressIndicator indicator) {
			def occurrences = new HashMap<String, Integer>().withDefault { 0 }
			def psiManager = PsiManager.getInstance(project)
			processRecursively(files) { VirtualFile file ->
				if (indicator.canceled) return STOP
				def psiFile = psiManager.findFile(file)
				if (psiFile instanceof PsiElement) {
					forEachIdentifierIn(psiFile) { PsiIdentifier psiIdentifier ->
						occurrences.put(psiIdentifier.text, occurrences[psiIdentifier.text] + 1)
					}
				}
				CONTINUE
			}
			occurrences
		}

		private static forEachIdentifierIn(PsiElement psiElement, Closure closure) {
			psiElement?.accept(new PsiRecursiveElementVisitor() {
				@Override void visitElement(PsiElement element) {
					if (element instanceof PsiIdentifier) closure.call(element)
					super.visitElement(element)
				}
			})
		}
	}

	private static class TextualWordOccurrences implements WordCloudSource {

		@Override Map<String, Integer> wordOccurrencesFor(List<VirtualFile> files, ProgressIndicator indicator) {
			def wordOccurrences = new HashMap<String, Integer>().withDefault { 0 }

			processRecursively(files) { VirtualFile file ->
				if (indicator.canceled) return STOP
				analyzeFile(file, wordOccurrences)
				CONTINUE
			}
			wordOccurrences.entrySet().removeAll { it.key == "def" || it.key == "new" }
			show(wordOccurrences)
			wordOccurrences
		}

		private static void analyzeFile(VirtualFile file, wordOccurrences) {
			if (file.isDirectory()) return
			if (file.extension != "groovy" && file.extension != "java") return

			def text = file.inputStream.readLines()

			// drop apache license header
			if (text.size() > 2 &&
					text[0].contains("/*") &&
					text[1].contains("Copyright 2000-2012 JetBrains s.r.o.")) {
				text = text.drop(15)
			}

			text.each { line ->
				if (line.startsWith("import")) return
				line.split(/[\s!{}\[\]+-<>()\/\\,"'@&$=*]/).findAll { !it.empty }.each { word ->
					wordOccurrences.put(word, wordOccurrences[word] + 1)
				}
			}
		}
	}

	static class FileProcessing {
		public static final STOP = "STOP"
		public static final CONTINUE = null

		static boolean processRecursively(List<VirtualFile> files, Closure process) {
			if (files == null || files.empty) return CONTINUE
			for (file in files) {
				if (process(file) == STOP) return STOP
				if (processRecursively(file?.children?.toList(), process) == STOP) return STOP
			}
			CONTINUE
		}
	}

}
