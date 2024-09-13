package applet;

public enum Type
{
    ISD("ISD (Issuer Security Domain)"),
    SSD("SSD (Supplementary Security Domain)"),
    APPLET("APP (Applet)"),
    PACKAGE("PKG (Package)");

    private final String name;

    Type(String name)
    {
        this.name = name;
    }

    @Override
    public String toString()
    {
        return this.name;
    }
}