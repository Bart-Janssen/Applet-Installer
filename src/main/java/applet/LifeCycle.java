package applet;

public enum LifeCycle
{
    INSTALLED("Installed"),
    SELECTABLE("Selectable"),
    UNKNOWN("Unknown"),
    LOCKED("Locked"),
    //Only for packages
    LOADED("Loaded"),
    //Only used for ISD
    TERMINATED("Terminated"),
    SECURED("Secured"),
    INITIALIZED("Initialized"),
    OP_READY("Op Ready"),
    APPLICATION_SPECIFIC("Application specific"),
    //Only for SSD
    PERSONALIZED("Personalized");

    private final String name;

    LifeCycle(String name)
    {
        this.name = name;
    }

    @Override
    public String toString()
    {
        return this.name;
    }
}