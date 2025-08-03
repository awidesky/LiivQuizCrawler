package io.github.awidesky.liivQuizCrawler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class HTML {
	
	private static Charset htmlCharset = StandardCharsets.UTF_8;
	
	public static String[] getText(String url) {
		return openURLconnection(url, s -> s.toArray(String[]::new));
	}
	public static <T> List<T> getTextFilter(String url, Function<String, T> map) {
		return openURLconnection(url, s -> s.map(map).filter(Objects::nonNull).toList());
	}
	public static <T> Optional<T> getTextFilterFirstOptional(String url, Function<String, T> map) {
		return openURLconnection(url, s -> s.map(map).filter(Objects::nonNull).findFirst());
	}
	public static <T> T getTextFilterFirst(String url, Function<String, T> map) {
		return getTextFilterFirstOptional(url, map).orElse(null);
	}
	public static String[] getTextUntilFind(String url, String str) {
		return openURLconnection(url, l -> l.takeWhile(s -> s.contains(str)).toArray(String[]::new));
	}
	
	public static String encodeURL(String link) {
		return URLDecoder.decode(link, StandardCharsets.UTF_8);
	}
	
	public static String getQuizlink(String listLink, Pattern linkPattern, int group) {
		return IntStream.range(1, 5).mapToObj(i -> findPatternFromList(listLink + i, linkPattern, group))
				.filter(Optional::isPresent).map(Optional::get).findFirst().orElseGet(() -> {
					Main.println("Cannot find article \"" + linkPattern + "\" from : " + listLink);
					return null;
				});
	}
	
	private static Optional<String> findPatternFromList(String link, Pattern linkPattern, int group) {
		return HTML.getTextFilterFirstOptional(link, s -> {
			Matcher m = linkPattern.matcher(s);
			if(m.find()) {
				return m.group(group);
			} else {
				return null;
			}
		});
	}
	
	private static <T> T openURLconnection(String url, Function<Stream<String>, T> f) {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(new URL(url).openConnection().getInputStream(), htmlCharset))) {
			return f.apply(br.lines());
		} catch (IOException e) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			Main.println(sw.toString());
			return null;
		}
	}
}
