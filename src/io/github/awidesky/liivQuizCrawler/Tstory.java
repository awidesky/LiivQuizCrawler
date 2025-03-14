package io.github.awidesky.liivQuizCrawler;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public class Tstory {

	private static final String listLink = "https://bookshelf-journey.tistory.com/category";
	
	public static String getHanaQuizAnswer(String today) {
		return getQuiz("하나원큐 축구Play 퀴즈HANA", today);
	}
	public static String getKBQuizAnswer(String today) {
		return getQuiz("KB 스타뱅킹 스타퀴즈", today);
	}

	private static String getQuiz(String title, String today) {
		String link = getQuizlink("\\[" + title + "\\] " + today);
		System.out.println(HTML.getTextFilterFirst(link, s -> {
			Pattern p = Pattern.compile("<title>(.*?)</title>");
			Matcher m = p.matcher(s);
			if(m.find()) return m.group(1);
			else return null;
		}) + " : " + encodeURL(link));
		String ret = getQuizAnswer(link);
		System.out.println(title + " : " + ret);
		return ret;
		
	}
	
	private static String encodeURL(String link) {
		return URLDecoder.decode(link, StandardCharsets.UTF_8);
	}
	
	private static String getQuizlink(String title) {
		Pattern linkPattern  = Pattern.compile(".*\"item\":\\{\"@id\":\"(.*?)\",\"name\":\"" + title);

		return IntStream.range(1, 5).mapToObj(i -> findPatternFromList(listLink + "?page=" + i, linkPattern))
				.filter(Optional::isPresent).map(Optional::get).findFirst().orElseGet(() -> {
					System.out.println("Cannot find \"" + title + "\" from : " + listLink);
					return null;
				});
	}
	
	private static Optional<String> findPatternFromList(String link, Pattern linkPattern) {
		return HTML.getTextFilterFirstOptional(link, s -> {
			Matcher m = linkPattern.matcher(s);
			if(m.find()) {
				return m.group(1);
			} else {
				return null;
			}
		});
	}
	private static String getQuizAnswer(String link) {
		String[] html = HTML.getText(link);
		Pattern titlePattern = Pattern.compile("^<blockquote(.*?)>((<b>(\\s*)퀴즈 정답(\\s*)</b>)|(<span(.*?)>(\\s*)퀴즈 정답(\\s*)</span>))(.*?)</blockquote>");
		Pattern pattern = Pattern.compile("((<b>(.+?)</b>)|(<span(.*?)>(.+?)</span>))");
		for(int i = 0; i < html.length; i++) {
			Matcher tm = titlePattern.matcher(html[i]);
			if(tm.find()) {
				Matcher matcher = pattern.matcher(html[i]);
				if(matcher.find() && matcher.find()) {
					Main.debug("Found tag : " + matcher.group(0));
					return Optional.ofNullable(matcher.group(3)).orElse(matcher.group(6))
								.replaceAll("(<(.*?)>)", "").replaceAll("[\\[\\]]", "").strip();
				} else {
					System.out.println("Cannot find " + pattern + " after find " + titlePattern);
					for(int j = i - 10; j < i + 10; j++)
						if(0 <= j && j < html.length) Main.debug("[" + j + "] " + html[j]);
					return tm.group(10).replaceAll("<(.*?)>", "").strip();
				}
			}
		}
		System.out.println("Cannot find " + titlePattern);
		return null;
	}

}
