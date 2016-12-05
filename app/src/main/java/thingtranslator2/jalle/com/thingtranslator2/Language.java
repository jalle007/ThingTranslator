package thingtranslator2.jalle.com.thingtranslator2;

public class Language {
    String langName;
    String langCode;


    public Language() {
    }

    public Language(String langName, String langCode) {
        this.langName = langName;
        this.langCode = langCode;
    }

    public String getLangName() {
        return langName;
    }

    public void setLangName(String langName) {
        this.langName = langName;
    }

    public String getlangCode() {
        return langCode;
    }

    public void setlangCode(String langCode) {
        this.langCode = langCode;
    }

    /**
     * Pay attention here, you have to override the toString method as the
     * ArrayAdapter will reads the toString of the given object for the name
     *
     * @return contact_name
     */
    @Override
    public String toString() {
        return langName;
    }
}
