package org.dcm4che3.data;

import org.dcm4che3.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.StringTokenizer;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @author Michael Backhaus <michael.backhaus@agfa.com>
 */
abstract public class PersonNameBase {

    private static final Logger LOG = LoggerFactory.getLogger(PersonNameBase.class);

    private final String[] fields = new String[15];

    public PersonNameBase(String s, boolean lenient) {
        if (s != null)
            parse(s, lenient);
    }

    private void parse(String s, boolean lenient) {
        int gindex = 0;
        int cindex = 0;
        StringTokenizer stk = new StringTokenizer(s, "^=", true);
        while (stk.hasMoreTokens()) {
            String tk = stk.nextToken();
            switch (tk.charAt(0)) {
                case '=':
                    if (++gindex > 2)
                        if (lenient) {
                            LOG.info(
                                    "illegal PN: {} - truncate illegal component group(s)", s);
                            return;
                        } else
                            throw new IllegalArgumentException(s);
                    cindex = 0;
                    break;
                case '^':
                    ++cindex;
                    handleEmptyComponent(gindex, cindex);
                    break;
                default:
                    if (cindex <= 4) {
                        set(gindex, cindex, tk);
                    } else if (lenient) {
                        if ((tk = trim(tk)) != null) {
                            LOG.info("illegal PN: {} - subsumes {}th component in suffix", s, cindex + 1);
                            set(gindex, 4, StringUtils.maskNull(get(gindex, 4), "") + ' ' + tk);
                        }
                    } else
                        throw new IllegalArgumentException(s);
            }
        }
    }

    protected abstract void handleEmptyComponent(int gindex, int cindex);

    /**
     * Set all components of a component group from encoded component group value.
     *
     * @param g component group
     * @param s encoded component group value
     */
    public void set(PersonName.Group g, String s) {
        int gindex = g.ordinal();
        if (s.indexOf('=') >= 0)
            throw new IllegalArgumentException(s);

        String[] ss = StringUtils.split(s, '^');
        if (ss.length > 5)
            throw new IllegalArgumentException(s);

        for (int cindex = 0; cindex < 5; cindex++) {
            fields[gindex * 5 + cindex] = cindex < ss.length ? trim(ss[cindex]) : null;
        }
    }

    public String toString() {
        int totLen = 0;
        PersonName.Group lastGroup = PersonName.Group.Alphabetic;
        for (PersonName.Group g : PersonName.Group.values()) {
            PersonName.Component lastCompOfGroup = PersonName.Component.FamilyName;
            for (PersonName.Component c : PersonName.Component.values()) {
                String s = get(g, c);
                if (s != null) {
                    totLen += s.length();
                    lastGroup = g;
                    lastCompOfGroup = c;
                }
            }
            totLen += lastCompOfGroup.ordinal();
        }
        totLen += lastGroup.ordinal();
        char[] ch = new char[totLen];
        int wpos = 0;
        for (PersonName.Group g : PersonName.Group.values()) {
            PersonName.Component lastCompOfGroup = PersonName.Component.FamilyName;
            for (PersonName.Component c : PersonName.Component.values()) {
                String s = get(g, c);
                if (s != null) {
                    int d = c.ordinal() - lastCompOfGroup.ordinal();
                    while (d-- > 0)
                        ch[wpos++] = '^';
                    d = s.length();
                    s.getChars(0, d, ch, wpos);
                    wpos += d;
                    lastCompOfGroup = c;
                }
            }
            if (g == lastGroup)
                break;
            ch[wpos++] = '=';
        }
        return new String(ch);
    }

    public String toString(PersonName.Group g, boolean trim) {
        int totLen = 0;
        PersonName.Component lastCompOfGroup = PersonName.Component.FamilyName;
        for (PersonName.Component c : PersonName.Component.values()) {
            String s = get(g, c);
            if (s != null) {
                totLen += s.length();
                lastCompOfGroup = c;
            }
        }
        totLen += trim ? lastCompOfGroup.ordinal() : 4;
        char[] ch = new char[totLen];
        int wpos = 0;
        for (PersonName.Component c : PersonName.Component.values()) {
            String s = get(g, c);
            if (s != null) {
                int d = s.length();
                s.getChars(0, d, ch, wpos);
                wpos += d;
            }
            if (trim && c == lastCompOfGroup)
                break;
            if (wpos < ch.length)
                ch[wpos++] = '^';
        }
        return new String(ch);
    }

    public String get(PersonName.Component c) {
        return get(PersonName.Group.Alphabetic, c);
    }

    public String get(PersonName.Group g, PersonName.Component c) {
        return get(g.ordinal(), c.ordinal());
    }

    public void set(PersonName.Component c, String s) {
        set(PersonName.Group.Alphabetic, c, s);
    }

    public void set(PersonName.Group g, PersonName.Component c, String s) {
        set(g.ordinal(), c.ordinal(), s);
    }

    protected String get(int gindex, int cindex) {
        return fields[gindex * 5 + cindex];
    }

    protected void set(int gindex, int cindex, String s) {
        fields[gindex * 5 + cindex] = trim(s);
    }

    public boolean isEmpty() {
        for (PersonName.Group g : PersonName.Group.values())
            if (contains(g))
                return false;
        return true;
    }

    public boolean contains(PersonName.Group g) {
        for (PersonName.Component c : PersonName.Component.values())
            if (contains(g, c))
                return true;
        return false;
    }

    public boolean contains(PersonName.Group g, PersonName.Component c) {
        return get(g, c) != null;
    }

    public boolean contains(PersonName.Component c) {
        return contains(PersonName.Group.Alphabetic, c);
    }

    private static String trim(String s) {
        if (s == null) return s;
        return s.trim();
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(fields);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (!(obj instanceof PersonNameBase))
            return false;

        PersonNameBase other = (PersonNameBase) obj;
        return Arrays.equals(fields, other.fields);
    }

    public interface PersonNameFactory {
        PersonNameBase create(String s, boolean lenient);
    }

}
