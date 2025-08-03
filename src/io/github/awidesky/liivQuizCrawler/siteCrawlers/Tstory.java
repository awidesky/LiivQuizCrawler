package io.github.awidesky.liivQuizCrawler.siteCrawlers;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.awidesky.liivQuizCrawler.HTML;
import io.github.awidesky.liivQuizCrawler.Main;

public class Tstory {

	private static final String listLink = "https://bookshelf-journey.tistory.com/category?page=" ;

	public static String getHanaQuizAnswer() {
		return getQuiz("하나원큐 축구Play 퀴즈HANA");
	}
	public static String getKBQuizAnswer() {
		return getQuiz("KB 스타뱅킹 스타퀴즈");
	}
	public static String getKBPayQuizAnswer() {
		return getQuiz("KB Pay 오늘의 퀴즈");
	}
	public static String getSOLQuizAnswer() {
		return getQuiz("신한 슈퍼 SOL 출석 퀴즈");
	}
	public static String getSOLBaseballQuizAnswer() {
		return getQuiz("신한 슈퍼 SOL 야구/상식 쏠퀴즈");
	}

	private static String getQuiz(String title) {
		String link = HTML.getQuizlink(listLink, Pattern.compile(".*\"item\":\\{\"@id\":\"(.*?)\",\"name\":\"" + title), 1);
		String ret = null;
		
		if(link != null) {
			Pattern p = Pattern.compile("<title>(.*?)</title>");
			Main.println(HTML.getTextFilterFirst(link, s -> {
				Matcher m = p.matcher(s);
				if(m.find()) return m.group(1);
				else return null;
			}) + " : " + HTML.encodeURL(link));
			Main.debug("Raw url : " + link);
			ret = getQuizAnswer(link);
		}
		Main.println(title + " : " + ret);
		return replaceChar(ret);
		
	}
	
	private static Map<String, String> replace = Map.of("①", "1.", "②", "2.", "③", "3.", "④", "4.", "⑤", "5.");
	private static String replaceChar(String str) {
		if(str == null) return null;
		
		for(Map.Entry<String, String> e : replace.entrySet()) {
			str = str.replace(e.getKey(), e.getValue());
		}
		return str;
	}
	
	private static String getQuizAnswer(String link) {
		String[] html = HTML.getText(link);
		Main.debug("HTML for quiz loaded, lines : " + html.length + ", approx bytes : " + Arrays.stream(html).parallel().mapToInt(s -> s.length() * 2).sum());
		Pattern titlePattern = Pattern.compile("^\\s*<blockquote(.*?)>((<b>(\\s*)퀴즈\\s*정답(\\s*)</b>)|(<span(.*?)>(\\s*)퀴즈\\s*정답(\\s*)</span>))(.*?)</blockquote>");
		Pattern pattern = Pattern.compile("(<span(.*?)>(.+?)</span>)");
		for(int i = 0; i < html.length; i++) {
			Matcher tm = titlePattern.matcher(html[i]);
			if(tm.find()) {
				Matcher matcher = pattern.matcher(html[i]);
				Main.debug("Found line : " + tm.group(0));
				if(matcher.find() && matcher.find()) {
					Main.debug("Found tag : " + matcher.group(0));
					return Optional.ofNullable(matcher.group(3)).get()
								.replaceAll("(<(.*?)>)", "").replaceAll("[\\[\\]]", "").strip();
				} else {
					Main.println("Cannot find content " + pattern + " after finding " + titlePattern);
					for(int j = i - 10; j < i + 10; j++)
						if(0 <= j && j < html.length) Main.debug("[" + j + "] " + html[j]);
					return tm.group(10).replaceAll("<(.*?)>", "").strip();
				}
			}
		}
		Main.println("Cannot find title " + titlePattern);
		if(Main.isDebug()) {
			Main.debug("All blockquotes :");
			Pattern blockquote = Pattern.compile("<blockquote(.*?)>(.*?)</blockquote>");
			Arrays.stream(html)
				.parallel()
				.filter(s -> blockquote.matcher(s).find())
				.forEach(Main::debug);
			Main.debug("Blockquotes end\n");
		}
		String ret = null;
		Main.println("Possible answer :");
		Pattern puntPattern = Pattern.compile("(.*)퀴즈\\s*정답(.{1,30})(.*)");
		for(int i = 0; i < html.length; i++) {
			Matcher m = puntPattern.matcher(html[i]);
			if(m.find()) {
				String str = m.group(2).strip();
				if(ret == null && replace.keySet().stream().anyMatch(str::contains))
					ret = str.replaceAll("\\s*앱테크 포인트 모으기.*", "");
				Main.println(str);
			}
		}
		return ret;
	}

}
