package io.github.awidesky.liivQuizCrawler.siteCrawlers;

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import io.github.awidesky.liivQuizCrawler.HTML;
import io.github.awidesky.liivQuizCrawler.Main;

public class Tipistip {

	private static final String listLink = "https://www.tipistip.com/bbs/board.php?bo_table=quiz&page=" ;
	private static final String today = Main.getDate("[YYYY년 MM월 dd일]");
	
	public static String getHanaQuizAnswer() {
		return getQuiz("기타퀴즈] 하나원큐 (오른쪽 하단 메뉴 &gt; 이벤트/스포츠 &gt; 하나원큐 축구Play &gt; 퀴즈HANA)");
	}
	public static String getKBQuizAnswer() {
		return getQuiz("KB 스타뱅킹] 스타퀴즈 (메뉴 &gt; 생활/혜택 &gt; 혜택 &gt; 이벤트)");
	}
	public static String getKBPayQuizAnswer() {
		return getQuiz("리브메이트 퀴즈] 오늘의 퀴즈 (KB Pay 앱으로 통합)");
	}

	private static String getQuiz(String title) {
		Pattern linkPattern = Pattern.compile(".*<a href=\\\"(.*?)\\\">.*");
		String link = IntStream.range(1, 5).mapToObj(i -> listLink + i)
				.map(li -> {
					String arr[] = HTML.getText(li);
					for(int i = 0; i < arr.length; i++) {
						if(arr[i].contains(title)) {
							String s = arr[i-1].replace("&amp;", "&");
							Main.debug("Found title : " + arr[i].replace("</a>", "").strip());
							Main.debug("Line before : " + arr[i-1].strip());
							Matcher m = linkPattern.matcher(s);
							if(m.find()) {
								return m.group(1);
							} else {
								Main.println("Found " + title + " but not " + linkPattern);
							}
						}
					}
					Main.println("Cannot find title \"" + title + "\" from : " + li);
					return null;
				})
				.filter(Objects::nonNull).findFirst().orElse(null); 
		String ret = null;
		
		
		if(link != null) {
			Pattern p = Pattern.compile("<title>(.*?)</title>");
			String t = HTML.getTextFilterFirst(link, s -> {
				Matcher m = p.matcher(s);
				if(m.find()) return m.group(1);
				else return null;
			});
			Main.println(t + " : " + HTML.encodeURL(link));
			Main.debug("Raw url : " + link);
			if(!t.contains(today)) {
				Main.println("!!!Wrong Title!!! : " + t);
				Main.println("!!!Should contain : " + today);
				return null;
			}
			ret = getQuizAnswer(link);
		}
		Main.println(title + " : " + ret);
		return ret;
		
	}
	
	private static String getQuizAnswer(String link) {
		String[] html = HTML.getText(link);
		Main.debug("HTML for quiz loaded, lines : " + html.length + ", approx bytes : " + Arrays.stream(html).parallel().mapToInt(s -> s.length() * 2).sum());
		
		Pattern startPattern = Pattern.compile("\\Q<!-- 본문 내용 시작 { -->\\E");
		int i = 0;
		for(; i < html.length; i++) {
			Matcher tm = startPattern.matcher(html[i]);
			if(tm.find()) break;
		}
		if(i == html.length) { //Failed
			Main.println("Cannot find content " + startPattern);
			return null;
		}
		
		Pattern pattern = Pattern.compile("정답 : (.*?)<.*");
		for(int j = i; j < html.length; j++){
				Matcher matcher = pattern.matcher(html[j]);
				if(matcher.find()) {
					Main.debug("Found line : " + html[j].strip());
					return matcher.group(1).strip();
				}
		}
		
		Main.println("Cannot find content " + pattern + " after finding " + startPattern);
		for(int j = i; j < i + 5 && j < html.length; j++) {
				Main.debug("[" + j + "] " + html[j]);
				if(html[i].contains("본문 내용 끝")) break;
		}
		return null;
	}
}
