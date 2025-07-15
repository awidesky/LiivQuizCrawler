package io.github.awidesky.liivQuizCrawler;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class Main {

	private static boolean debug = false;
	private static Object[] oneLiner = new String[5];
	private static Consumer<String> out = s -> System.out.print(s);
	
	public static void main(String[] args) {

		for(int i = 0; i < args.length; i++) {
			switch (args[i]) {
			case "--getText":
				String link;
				if (i < args.length)
					link = args[i + 1];
				else {
					print("Enter link to print > ");
					Scanner sc = new Scanner(System.in);
					link = sc.nextLine();
					sc.close();
				}
				for (String s : HTML.getText(link))
					println(s);
				return;
				
			case "--debug":
				debug = true;
				break;
				
			case "--gui":
				Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
				JFrame f = new JFrame("LiivQuizCrawler");
				f.setSize(1000, 600);
				f.setLocation(dim.width/2-f.getSize().width/2, dim.height/2-f.getSize().height/2);
				f.setLayout(new BorderLayout());
				JTextArea ja = new JTextArea();
				ja.setEditable(false);
				ja.setLineWrap(true);
				out = s -> { ja.append(s); ja.setCaretPosition(ja.getText().length()); };
				JScrollPane sp = new JScrollPane(ja, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
				f.add(sp, BorderLayout.CENTER);
				f.setVisible(true);
				f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				break;
			}
			
		}
		
		
		final String today = new SimpleDateFormat("M월 d일").format(new Date());
		debug("Today : " + today);
		String[] arr = find_quiz("쏠퀴즈", 7);
		if(arr != null) {
			for (int i = 1; i < arr.length; i++) {
				if(arr[i].contains("퀴즈팡팡")) {
					continue;
				}
				if (arr[i].contains("출석퀴즈")) {
					printf("%s : %s\n", arr[i], arr[i + 1]);
					oneLiner[0] = arr[i + 1];
				} else if (arr[i].contains("쏠퀴즈")) {
					printf("%s : %s\n", arr[i], arr[i + 1]);
					oneLiner[1] = arr[i + 1];
				}
			}
		}
		if(oneLiner[0] == null) {
			oneLiner[0] = Tstory.getSOLQuizAnswer(today);
		}
		if(oneLiner[1] == null) {
			oneLiner[1] = Tstory.getSOLBaseballQuizAnswer(today);
		}
		
		println();
		oneLiner[2] = Tstory.getKBPayQuizAnswer(today);
		if (oneLiner[2] == null) {
			arr = find_quiz("KB Pay 리브메이트 오늘의 퀴즈 정답 " + today.replace(" ", ""), 2);
			if (arr != null) {
				println(arr[0] + " : " + arr[1]);
				oneLiner[2] = arr[1];
			}
		}

		println();
		oneLiner[3] = Tstory.getHanaQuizAnswer(today);
		
		println();
		oneLiner[4] = Tstory.getKBQuizAnswer(today);
		
		println("\n");
		printf("신한 \"%s\"  \"%s\"  리브 \"%s\"  하나 \"%s\"  KB \"%s\"", oneLiner);
		println();
	}
	
	private static final String newsList = "https://www.bntnews.co.kr/article/list/bnt005005000"; 
	private static String[] find_quiz(String title, int n) {
		try {
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
			println(matcher.group(1).replace("| bnt뉴스", "").strip() + " : " + sol);
			
			/* find quiz answer */
			pattern = Pattern.compile("((<strong>(.+?)</strong>)|(<b>(.+?)</b>))");
			int j = 0;
			String[] ret = new String[n]; Arrays.fill(ret, "");
			for(int i = 0; i < n;) {
				try {
					matcher = pattern.matcher(html[j++]);
				} catch (ArrayIndexOutOfBoundsException e) {
					println("Cannot find pattern more than " + i + "(" + n + "expected): \"" + pattern.pattern() + "\" from " + title);
					//Arrays.stream(html).forEach(System.out::println);
					break;
				}
				if(!matcher.find()) continue;
				
				do {
					ret[i] = Optional.ofNullable(matcher.group(3)).orElse(matcher.group(5)).strip();
					debug("ret[" + i + "] : " + ret[i]);
					i++;
				} while (matcher.find());
			}
			return ret;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
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
		println("Found \"" + arr[idx].strip() + "\" from line " + (idx + 1));
		for(int n = i; n < j; n++) println(arr[n]);
	}
	
	public static void debug(String str) {
		if(debug) println(str);
	}
	public static void println() {
		print("\n");
	}
	public static void println(String str) {
		print(str + "\n");
	}
	public static void printf(String str, Object... args) {
		print(str.formatted(args));
	}
	public static void print(String str) {
		out.accept(str);
	}
	
	public static boolean isDebug() {
		return debug;
	}
}
