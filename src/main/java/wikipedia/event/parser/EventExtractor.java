package wikipedia.event.parser;

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import java.time.MonthDay;
import java.time.Year;

import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import org.jsoup.select.Elements;

import wikipedia.event.model.Event;

/**
 * Class for querying events that occurred on a specific day of month. Events
 * are extracted from the English Wikipedia page about the specific day of
 * month.
 */
public class EventExtractor {

	private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("[G' ']y[' 'G]").withLocale(Locale.ENGLISH);

	/**
	 * Default value of the timeout for accessing Wikipedia pages.
	 */
	private static final int TIMEOUT = 5000;

	/**
	 * The timeout for accessing Wikipedia pages in milliseconds.
	 */
	private int timeout = TIMEOUT;

	/**
	 * Constructs a {@code EventExtractor} object with default timeout.
	 *
	 * @see #getTimeout()
	 * @see #setTimeout(int)
	 */
	public EventExtractor() {
	}

	/**
	 * Constructs a {@code EventExtractor} object with the timeout specified.
	 *
	 * @param timeout the timeout for accessing Wikipedia pages in
	 *                milliseconds
	 *
	 * @see #getTimeout()
	 * @see #setTimeout(int)
	 */
	public EventExtractor(int timeout) {
		this.timeout = timeout;
	}

	/**
	 * Returns the timeout for accessing Wikipedia pages.
	 *
	 * @return the timeout for accessing Wikipedia pages in milliseconds
	 */
	public int getTimeout() {
		return timeout;
	}

	/**
	 * Sets the timeout for accessing Wikipedia pages.
	 *
	 * @param timeout the timeout for accessing Wikipedia pages in
	 *                milliseconds
	 */
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	/**
	 * Returns the web page address of the Wikipedia page about the day of
	 * month specified.
	 *
	 * @param monthDay an object that wraps the month of year and the day
	 *                 of month
	 * @return the web page address of the Wikipedia page about the day of
	 *         month specified, as a string
	 */
	private String getWikipediaURL(MonthDay monthDay) {
		final String month = monthDay.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
		return String.format("http://en.wikipedia.org/wiki/%s_%d", month, monthDay.getDayOfMonth());
	}

	private Document getWikipediaPage(MonthDay monthDay) throws IOException {
		final String url = getWikipediaURL(monthDay);
		System.out.printf("Retrieving web page from %s%n", url);
		Document doc = Jsoup.connect(url).timeout(timeout).get();
		return doc;
	}

	private Elements getEventElements(Document doc) {
		Elements elements = doc.select("h2:has(#Events) + ul > li");
		System.out.printf("Found %d event(s)%n", elements.size());
		return elements;
	}

	private Event createEvent(Element element, MonthDay monthDay) {
		System.out.printf("Text to be parsed: %s%n", element.text());
		String[] textParts = element.text().split(" \u2013 ", 2);
		if (textParts.length != 2) {
			throw new IllegalArgumentException();
		}
		textParts[0] = textParts[0].trim();
		textParts[1] = textParts[1].trim();
		Year year = Year.parse(textParts[0], formatter);
		return new Event(year.atMonthDay(monthDay), textParts[1]);
	}

	/**
	 * Returns events that occurred on the day of month specified.
	 *
	 * @param monthDay an object that wraps the month of year and the day of
	 *                 month
	 * @return the list of objects that represent events that occurred on the
	 *         day of month specified
	 * @throws IOException if any I/O error occurs
	 */
	public List<Event> getEvents(MonthDay monthDay) throws IOException {
		Document doc = getWikipediaPage(monthDay);
		List<Event> events = new ArrayList<>();
		for (Element element: getEventElements(doc)) {
			try {
				Event event = createEvent(element, monthDay);
				events.add(event);
			} catch (Exception e) {
				System.out.printf("Skipped malformed element: %s%n", element);
			}
		}
		return events;
	}

	/**
	 * Returns events that occurred on the day of month specified.
	 *
	 * @param monthOfYear the month of the year (1&ndash;12)
	 * @param dayOfMonth the day of the month (1&ndash;31)
	 * @return the list of objects that represent events that occurred on the
	 *         day of month specified
	 * @throws IOException if any I/O error occurs
	 */
	public List<Event> getEvents(int monthOfYear, int dayOfMonth) throws IOException {
		return getEvents(MonthDay.of(monthOfYear, dayOfMonth));
	}

}
