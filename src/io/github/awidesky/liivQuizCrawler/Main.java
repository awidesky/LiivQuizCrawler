package io.github.awidesky.liivQuizCrawler;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public class Main {

	private static boolean debug = false;
	private static Object[] oneLiner = new String[3];
	
	public static void main(String[] args) {
		final String today = new SimpleDateFormat("M월 d일").format(new Date());
		debug("Today : " + today);
		String[] arr = find_quiz("쏠퀴즈", 7);
		for (int i = 1; i < arr.length; i++) {
			if(arr[i].contains("퀴즈팡팡")) {
				continue;
			}
			if (arr[i].contains("출석퀴즈")) {
				System.out.printf("%s : %s\n", arr[i], arr[i + 1]);
				oneLiner[0] = arr[i + 1];
			} else if (arr[i].contains("쏠퀴즈")) {
				System.out.printf("%s : %s\n", arr[i], arr[i + 1]);
				oneLiner[1] = arr[i + 1];
			}
		}
		
		System.out.println();
		arr = find_quiz("KB Pay 리브메이트 오늘의 퀴즈 정답 " + today.replace(" ", ""), 2);
		System.out.println(arr[0] + " : " + arr[1]);
		oneLiner[2] = arr[1];
		
		System.out.println("\n");
		System.out.printf("신한 \"%s\"  \"%s\"  리브 \"%s\"", oneLiner);
		System.out.println();
	}
	
	private static final String newsList = "https://www.bntnews.co.kr/article/list/bnt005005000"; 
	private static String[] find_quiz(String title, int n) {
		String[] html = HTML.getText(newsList);
		
		int ii = search(html, s -> s.contains(title));
		if(ii == -1) throw new RuntimeException("Cannot find \"" + title + "\"" + " from " + newsList);
		printSurroundings(html, ii);
		Pattern pattern = Pattern.compile("(/article/view/bnt\\d+)");
		Matcher matcher = pattern.matcher(html[ii - 1]);
		while (!matcher.find()) {
			matcher = pattern.matcher(html[(--ii) - 1]);
		}
		String sol = "https://www.bntnews.co.kr" + matcher.group(1);
		debug("Found quiz link : " + sol);

		html = HTML.getText(sol);
		/* find title */
		Pattern titlePattern = Pattern.compile("<title>(.*?)</title>");
		int t = search(html, s -> titlePattern.matcher(s).find());
		if(t == -1) throw new RuntimeException("Cannot find <title> from " + sol);
		matcher = titlePattern.matcher(html[t]);
		matcher.find();
		System.out.println(matcher.group(1).replace("| bnt뉴스", "").strip() + " : " + sol);
		
		/* find quiz answer */
		pattern = Pattern.compile("(<strong>(.+?)</strong>)");
		int j = 0;
		String[] ret = new String[n]; Arrays.fill(ret, "");
		for(int i = 0; i < n;) {
			try {
				matcher = pattern.matcher(html[j++]);
			} catch (ArrayIndexOutOfBoundsException e) {
				System.out.println("Cannot find pattern more than " + i + "(" + n + "expected): \"" + pattern.pattern() + "\" from " + title);
				//Arrays.stream(html).forEach(System.out::println);
				break;
			}
			if(!matcher.find()) continue;
			
			do {
				ret[i] = matcher.group(2).strip();
				debug("ret[" + i + "] : " + ret[i]);
				i++;
			} while (matcher.find());
		}
		return ret;
	}
	
	private static int search(String[] arr, Predicate<String> pred) {
		return IntStream.range(0, arr.length)
				   .filter(i -> pred.test(arr[i]))
				   .findFirst().orElse(-1);
	}

	private static void printSurroundings(String[] arr, int idx) {
		if(!debug) return;
		
		int i = Math.max(idx - 5, 0);
		int j = Math.min(idx + 5, arr.length);
		System.out.println("Found \"" + arr[idx].strip() + "\" from line " + (idx + 1));
		for(int n = i; n < j; n++) System.out.println(arr[n]);
	}
	
	public static void debug(String str) {
		if(debug) System.out.println(str);
	}
}
