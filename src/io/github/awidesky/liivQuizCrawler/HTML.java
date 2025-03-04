package io.github.awidesky.liivQuizCrawler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public class HTML {
	public static String[] getText(String url) {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(new URL(url).openConnection().getInputStream()))) {
 
            String inputLine;
            List<String> l = new LinkedList<>();
            while ((inputLine = br.readLine()) != null) {
                //System.out.println(inputLine);
            	l.add(inputLine);
            }
            return l.toArray(String[]::new);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
	}
	public static <T> List<T> getTextFilter(String url, Function<String, T> map) {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(new URL(url).openConnection().getInputStream()))) {
			 return br.lines().map(map).filter(Objects::nonNull).toList();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
	}
	public static <T> Optional<T> getTextFilterFirstOptional(String url, Function<String, T> map) {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(new URL(url).openConnection().getInputStream()))) {
			return br.lines().map(map).filter(Objects::nonNull).findFirst();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	public static <T> T getTextFilterFirst(String url, Function<String, T> map) {
		return getTextFilterFirstOptional(url, map).orElse(null);
	}
	public static String[] getTextUntilFind(String url, String str) {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(new URL(url).openConnection().getInputStream()))) {
			return br.lines().takeWhile(s -> s.contains(str)).toArray(String[]::new);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
}
