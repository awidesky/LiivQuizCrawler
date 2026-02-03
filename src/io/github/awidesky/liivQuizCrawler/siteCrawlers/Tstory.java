package io.github.awidesky.liivQuizCrawler.siteCrawlers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import io.github.awidesky.liivQuizCrawler.HTML;
import io.github.awidesky.liivQuizCrawler.Main;

public class Tstory {

	private static final String listLink = "https://bookshelf-journey.tistory.com/category?page=";
	private static final String today = Main.getDate("M월 d일");

	public static String getClimateQuizAnswer() {
		return getQuiz("기후행동 기회소득", Tstory::quiz_fetch_csv);
	}
	public static String getHanaQuizAnswer() {
		return getQuiz("하나원큐 축구Play 퀴즈HANA", Tstory::quiz_fetch_csv);
	}
	public static String getKBQuizAnswer() {
		return getQuiz("KB 스타뱅킹 스타퀴즈", Tstory::quiz_fetch_csv);
	}
	public static String getKBPayQuizAnswer() {
		String ret = getQuiz("KB Pay 오늘의 퀴즈", Tstory::quiz_fetch_csv);
		if(ret == null && Main.isDebug()) printAllItems();
		if(("정답은 " + today + " 10:00시에 오픈합니다.").equals(ret)) {
			Main.println("Quiz answer not open yet!");
			ret = null;
		}
		return ret;
	}
	public static String getSOLQuizAnswer() {
		return getQuiz("신한 슈퍼 SOL 출석 퀴즈");
	}
	public static String getSOLBaseballQuizAnswer() {
		return getQuiz("신한 슈퍼 SOL 야구/상식 쏠퀴즈");
	}
	
	public static void printAllItems() {
		Main.println("All items found in " + listLink);
		getQuizTitleMatchers(m -> true, m -> m.group() + "\n" + HTML.encodeURL(m.group(1)) + " : " + m.group(2))
				.forEach(System.out::println);
		Main.println("List end");
	}

	private static Pattern linkPatten = Pattern.compile("\"item\":\\{\"@id\":\"(.*?)\",\"name\":\"(.*?)\\}");
	private static Stream<String> getQuizTitleMatchers(Predicate<Matcher> p, Function<Matcher, String> mapper) {
		return IntStream.range(1, 5).mapToObj(i -> HTML.getText(listLink + 1)).flatMap(Arrays::stream).map(linkPatten::matcher)
				.map(m -> {
					List<String> l = new LinkedList<>();
					while(m.find()) {
						if(p.test(m)) {
							l.add(mapper.apply(m));
						}
					}
					return l;
				}).flatMap(List::stream);
	}
	private static String getQuiz(String title) {
		return getQuiz(title, Tstory::blockquote);
	}
	private static String getQuiz(String title, Function<String[], String> finder) {
		String link = getQuizTitleMatchers(m -> m.group(2).contains(title), m -> m.group(1))
				.filter(Objects::nonNull).findFirst().orElseGet(() -> {
					Main.println("Cannot find article \"" + linkPatten.pattern() + "\" from : " + listLink);
					return null;
				});
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
			ret = getQuizAnswer(link, finder);
			if(ret != null) ret = ret.strip();
		}
		Main.println(title + " : " + ret);
		return Main.fixString(ret);
		
	}
	
	private static String getQuizAnswer(String link, Function<String[], String> finder) {
		String[] html = HTML.getText(link);
		Main.debug("HTML for quiz loaded, lines : " + html.length + ", approx bytes : " + Arrays.stream(html).parallel().mapToInt(s -> s.length() * 2).sum());
		
		String ret = finder.apply(html);
		if(ret != null) return ret;
		
		Main.println("Possible answer :");
		Pattern puntPattern = Pattern.compile("(.*)퀴즈\\s*정답(.{1,30})(.*)");
		for(int i = 0; i < html.length; i++) {
			Matcher m = puntPattern.matcher(html[i]);
			if(m.find()) {
				String str = m.group(2).strip();
				if(Main.numbers.keySet().stream().anyMatch(str::contains))
					ret = str.replaceAll("\\s*앱테크 포인트 모으기.*", "");
				Main.println(str);
			}
		}
		return ret;
	}

	
	private static String blockquote(String[] html) {
		Pattern titlePattern = Pattern.compile("^\\s*<blockquote(.*?)>((<b>(\\s*)퀴즈\\s*정답(\\s*)</b>)|(<span(.*?)>(\\s*)퀴즈\\s*정답(\\s*)</span>))(.*?)</blockquote>");
		Pattern pattern = Pattern.compile("(<span(.*?)>(.+?)</span>)");
		for(int i = 0; i < html.length; i++) { // finderlist.get(0).find(html)v
			Matcher tm = titlePattern.matcher(html[i]);
			if(tm.find()) {
				Matcher matcher = pattern.matcher(html[i]);
				Main.debug("Found line : " + tm.group(0));
				if(matcher.find() && matcher.find()) {
					Main.debug("Found tag : " + matcher.group(0));
					return Optional.ofNullable(matcher.group(3)).get()
								.replaceAll("(<(.*?)>)", "").replaceAll("[\\[\\]]", "");
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
		return null;
	}

	@SuppressWarnings("unused")
	private static String QUIZ_DATA(String[] html) {
		Pattern quizdataPattern = Pattern.compile("^\\s*const QUIZ_DATA = \\{");
		Pattern pattern = Pattern.compile("answer: \\\"(.*?)\\\",");
		for(int i = 0; i < html.length; i++) {
			Matcher tm = quizdataPattern.matcher(html[i]);
			if(tm.find()) {
				for(int j = i; j < html.length; j++) {
					Matcher matcher = pattern.matcher(html[j]);
					if(matcher.find()) {
						Main.debug("Found line : " + tm.group(0));
						return matcher.group(1);
					}
				}
			}
		}
		Main.println("Cannot find " + quizdataPattern);
		return null;
	}
	
	@SuppressWarnings("unused")
	private static String quiz_answer_text(String[] html) {
		Pattern pattern = Pattern.compile("<div\\s+class=\"quiz-answer-text-area\"\\s*>\\s*<h3\\s+id=\"dynamic-answer\"\\s+class=\"quiz-answer-text text-size23\"\\s+data-ke-size=\"size23\">(.*?)</h3>\\s*</div>", Pattern.DOTALL);
		for(int i = 0; i < html.length; i++) {
			Matcher matcher = pattern.matcher(html[i]);
			if(matcher.find()) {
				Main.debug("Found line : " + matcher.group(0));
				return matcher.group(1);
			}
		}
		Main.println("Cannot find " + pattern.pattern());
	
		Pattern narrowPattern = Pattern.compile("\"quiz-answer-text\">(.*?)<");
		Main.debug("Check narrow pattern : " + narrowPattern.pattern());
		return Arrays.stream(html)
				.parallel()
				.map(narrowPattern::matcher)
				.filter(Matcher::find)
				.map(m -> m.group(0))
				.peek(Main::debug)
				.findAny().orElse(null);
	}
	
	private static String quiz_fetch_csv(String[] html) {
		String csvUrl = null, searchKey = null;
		Pattern csvUrlPattern = Pattern.compile("const\\s+SHEET_URL\\s*=\\s*\"([^\"]+)\"");
		Pattern seachKeyPattern = Pattern.compile("const\\s+SEARCH_KEY\\s*=\\s*\"([^\"]+)\"");
		for(int i = 0; i < html.length; i++) {
			Matcher csvUrlMatcher = csvUrlPattern.matcher(html[i]);
			Matcher seachKeyMatcher = seachKeyPattern.matcher(html[i]);
			if(seachKeyMatcher.find()) {
				searchKey = seachKeyMatcher.group(1);
				Main.debug("Found searchKey : " + searchKey);
			}
			if(csvUrlMatcher.find()) {
				csvUrl = csvUrlMatcher.group(1);
				Main.debug("Found csv file link : " + csvUrl);
			} 
		}
		if(csvUrl == null || searchKey == null)
			Main.println("csvUrl = %s, seachKey = %s".formatted(csvUrl, searchKey));

		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(new URL(csvUrl).openStream(), StandardCharsets.UTF_8))) {

			String line;
			boolean isHeader = true;

			// CSV 한 줄 파싱 (따옴표, 쉼표 처리 정규식)
			Pattern pattern = Pattern.compile("(?:^|,)(?:\"([^\"]*(?:\"\"[^\"]*)*)\"|([^\",]*))");

			while ((line = br.readLine()) != null) {
				if (isHeader) {
					isHeader = false;
					continue;
				}

				List<String> columns = new ArrayList<>();
				Matcher matcher = pattern.matcher(line);

				while (matcher.find()) {
					String value = matcher.group(1) != null ? matcher.group(1).replace("\"\"", "\"") : matcher.group(2);
					columns.add(value != null ? value.trim() : "");
				}

				if (!columns.isEmpty() && columns.get(0).equals(searchKey)) {
					return Main.fixString(columns.size() > 2 ? columns.get(2) : null);
				}
			}

			br.close();

		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			Main.println(sw.toString());
		}
		Main.println("%s not found in : ".formatted(searchKey, csvUrl));
		return null;
	}

	public static void check_quiz_CSV(List<String> searchKeys, String[] out, int offset) {
		AtomicReference<String[]> ref = new AtomicReference<>();
		Stream.of("기후행동 기회소득", "하나원큐 축구Play 퀴즈HANA", "KB 스타뱅킹 스타퀴즈", "신한 슈퍼 SOL 출석 퀴즈", "신한 슈퍼 SOL 야구/상식 쏠퀴즈",
				"KB Pay 오늘의 퀴즈").map(title -> getQuiz(title, arr -> {
					ref.set(arr);
					return "Visited to acquire quiz CSV file";
				})).filter(Objects::nonNull).findFirst().orElseGet(null); //TODO : check if null?
		quiz_fetch_csv_ALL(ref.get(), searchKeys, out, offset);
	}
	private static boolean quiz_fetch_csv_ALL(String[] html, List<String> searchKeys, String[] out, int offset) {
		String csvUrl = null;
		Pattern csvUrlPattern = Pattern.compile("const\\s+SHEET_URL\\s*=\\s*\"([^\"]+)\"");

		for (String line : html) {
			Matcher m = csvUrlPattern.matcher(line);
			if (m.find()) {
				csvUrl = m.group(1);
				Main.debug("Found csv file link : " + csvUrl);
				break;
			}
		}

		if (csvUrl == null) {
			Main.println(csvUrlPattern.pattern() + " not found!");
			return false;
		}

		// searchKey - index mapping
		Map<String, Integer> keyIndexMap = new HashMap<>();
		for (int i = 0; i < searchKeys.size(); i++) {
			keyIndexMap.put(searchKeys.get(i), i);
		}

		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(new URL(csvUrl).openStream(), StandardCharsets.UTF_8))) {

			String line;
			boolean isHeader = true;

			Pattern csvPattern = Pattern.compile("(?:^|,)(?:\"([^\"]*(?:\"\"[^\"]*)*)\"|([^\",]*))");

			while ((line = br.readLine()) != null) {

				if (isHeader) {
					isHeader = false;
					continue;
				}

				List<String> columns = new ArrayList<>();
				Matcher matcher = csvPattern.matcher(line);

				while (matcher.find()) {
					String value = matcher.group(1) != null ? matcher.group(1).replace("\"\"", "\"") : matcher.group(2);
					columns.add(value != null ? value.trim() : "");
				}

				if (columns.isEmpty())
					continue;

				// desired searchKey?
				String key = columns.get(0);
				Integer index = keyIndexMap.get(key);
				if (index == null)
					continue;

				String answer = Main.fixString(columns.size() > 2 ? columns.get(2) : null);
				Main.println("Found %s : %s".formatted(key, answer));
				out[index + offset] = answer;
			}

			return true;
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			Main.println(sw.toString());
			return false;
		}
	}
}
