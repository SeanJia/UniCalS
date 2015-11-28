/**
 * A class for the comparator regarding cookies used in unical
 * @author Zhiwei Jia
 */

import java.util.Comparator;
import javax.servlet.http.Cookie;

public class CookieComparator implements Comparator<Cookie> {
	public int compare(Cookie a, Cookie b) {
		int hitA, hitB;
		try {
			hitA = Integer.parseInt(a.getName().substring(12, a.getName().length()));
		} catch (NumberFormatException e) {
			hitA = 0;
		}
		try {
			hitB = Integer.parseInt(b.getName().substring(12, b.getName().length()));
		} catch (NumberFormatException e) {
			hitB = 0;
		}
		if (hitA < hitB)
			return -1;
		else if (hitA == hitB)
			return 0;
		else
			return 1;
	}
}
