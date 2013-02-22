import com.intellij.ide.BrowserUtil
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiRecursiveElementVisitor
import http.Util
import org.junit.Test

import static WordCloud.FileProcessing.*
import static com.intellij.notification.NotificationType.ERROR
import static http.Util.*
import static intellijeval.PluginUtil.*
import static java.lang.Character.isUpperCase
/**
 * User: dima
 * Date: 18/11/2012
 */
class WordCloud {
	private static final String HTTP_SERVER = "WordCloud_HttpServer"

	static def showWordCloudFor(List<VirtualFile> files, String pluginPath) {
		generateCloud(new TextualWordOccurrences(), files) { String wordOccurrencesAsJSON ->
			setGlobalVar("wordCloudData", [
					"/static_cloud/words.json": wordOccurrencesAsJSON,
					"/static_cloud/files.json": convertToJSON(files)])
			def requestHandler = { getGlobalVar("wordCloudData").get(it) }
			def server = Util.restartHttpServer(HTTP_SERVER, pluginPath, requestHandler, { log(it, ERROR) })
			BrowserUtil.launchBrowser("http://localhost:${server.port}/wordcloud.html?static_cloud")
		}
	}

	static def updateCurrentEditorWordCloud(VirtualFile file, String pluginPath) {
		generateCloud(new TextualWordOccurrences(), [file]) { String wordOccurrencesAsJSON ->
			setGlobalVar("currentEditorWordCloudData", [
					"/live_cloud/words.json": wordOccurrencesAsJSON,
					"/live_cloud/files.json": convertToJSON([file])])
			def requestHandler = { getGlobalVar("currentEditorWordCloudData").get(it) }
			Util.restartHttpServer(HTTP_SERVER, pluginPath, requestHandler, { log(it, ERROR) })
		}
	}


	static def openCurrentEditorBrowser() {
		def server = getGlobalVar(HTTP_SERVER)
		if (server != null) BrowserUtil.launchBrowser("http://localhost:${server.port}/wordcloud.html?live_cloud")
	}

	private static def generateCloud(WordCloudSource wordCloudSource, List<VirtualFile> files, Closure whenDone) {
		def createWordCloudAsJson = { ProgressIndicator indicator ->
			runReadAction {
				try {
					convertToJSON(wordCloudSource.wordOccurrencesFor(files, indicator))
				} catch (Exception e) {
					log(e, ERROR)
					""
				}
			}
		}
		doInBackground("Preparing word cloud...", true, createWordCloudAsJson, whenDone)
	}

	static String convertToJSON(List<VirtualFile> files) {
		"{\"files\": [" + files.collect{'"' + it.path + '"'}.join(",") + "]}"
	}

	static String convertToJSON(Map wordOccurrences) {
		if (wordOccurrences.isEmpty()) return '{"words": []}'

		def min = wordOccurrences.min { it.value }.value
		def max = wordOccurrences.max { it.value }.value
        def range = (max != min ? max - min : 1)
		def normalizedSizeOf = { entry ->
			def word = entry.key
			def size = entry.value
			if (word.size() < 5) size += (max * 0.3) // this is to make shorter words more noticeable


            Math.round((double) 5 + ((size - min) * 75 / range))
		}
		"""{"words": [
${wordOccurrences.entrySet().sort{ -it.value }.take(600).collect { '{"text": "' + it.key + '", "size": ' + normalizedSizeOf(it) + '}' }.join(",\n")}
]}
"""
	}


	private interface WordCloudSource {
		Map<String, Integer> wordOccurrencesFor(List<VirtualFile> files, ProgressIndicator indicator)
	}

	private static class TextualWordOccurrences implements WordCloudSource {

		@Override Map<String, Integer> wordOccurrencesFor(List<VirtualFile> files, ProgressIndicator indicator) {
			def wordOccurrences = new HashMap<String, Integer>().withDefault { 0 }

			processRecursively(files) { VirtualFile file ->
				if (indicator?.canceled) return STOP
				analyzeFile(file, wordOccurrences)
				CONTINUE
			}
			wordOccurrences.entrySet().removeAll { it.key == "def" || it.key == "new" }
			wordOccurrences
		}

		private static void analyzeFile(VirtualFile file, wordOccurrences) {
			if (file == null) return
			if (file.isDirectory()) return
			if (file.fileType.isBinary()) return
//			if (file.extension != "groovy" && file.extension != "java") return

			def text = file.inputStream.readLines()

			// drop apache license header
			if (text.size() > 2 &&
					text[0].contains("/*") &&
					text[1].contains("Copyright 2000-2012 JetBrains s.r.o.")) {
				text = text.drop(15)
			}

			text.each { line ->
				if (line.startsWith("import")) return
				line.split(/[\s!{}\[\]+-<>()\/\\,"'@&$=*\|\?]/).findAll{ !it.empty }.each { word ->
					def subWords = splitByCamelHumps(word).collect{ splitByUnderscores(it.toLowerCase()) }.flatten()
					subWords.each {
						wordOccurrences.put(it, wordOccurrences[it] + 1)
					}
				}
			}
		}
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

	private static List<String> splitByCamelHumps(String s) {
		def words = []
		def wordStart = 0
		for (int i = 0; i < s.length() - 1; i++) {
			char c = s.charAt(i)
			char nextC = s.charAt(i + 1)
			if (i != wordStart && isUpperCase(c) && !isUpperCase(nextC)) {
				words << new String(s.substring(wordStart, i))
				wordStart = i
			}
		}
		words << new String(s.substring(wordStart))
		words
	}

	private static List<String> splitByUnderscores(String s) {
		def words = []
		int i
		while ((i = s.indexOf('_')) != -1) {
			if (i > 0) words << new String(s.substring(0, i))
			s = s.substring(i + 1)
		}
		if (s.length() > 0) words << new String(s)
		words
	}

	@Test void splittingByUnderscores() {
		assert splitByUnderscores("word") == ["word"]
		assert splitByUnderscores("two_words") == ["two", "words"]
		assert splitByUnderscores("more_than_two_words") == ["more", "than", "two", "words"]
		assert splitByUnderscores("double__underscore") == ["double", "underscore"]
		assert splitByUnderscores("_trailing_underscores_") == ["trailing", "underscores"]
	}

	@Test void splittingByCamelHumps() {
		assert splitByCamelHumps("word") == ["word"]
		assert splitByCamelHumps("twoWords") == ["two", "Words"]
		assert splitByCamelHumps("moreThanTwoWords") == ["more", "Than", "Two", "Words"]

		assert splitByCamelHumps("ClassNames") == ["Class", "Names"]

		assert splitByCamelHumps("UPPERCASE") == ["UPPERCASE"]
		assert splitByCamelHumps("isON") == ["isON"]
		assert splitByCamelHumps("isAt") == ["is", "At"]
	}
}
