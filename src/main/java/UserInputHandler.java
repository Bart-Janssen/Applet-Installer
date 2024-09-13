import applet.Applet;
import pro.javacard.CAPFile;
import smartcard.SmartCard;
import smartcard.channel.ChannelType;
import smartcard.factory.SmartCardVersion1Factory;
import smartcard.factory.SmartCardVersion3Factory;
import smartcard.factory.SmartCardVersion2Factory;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.TerminalFactory;
import java.io.File;
import java.nio.file.Files;
import java.util.*;

public class UserInputHandler
{

    private final String appletLocation = System.getProperty("user.home") + "\\Desktop\\PQC-project\\Applet-RAM-benchmark\\applet\\";
//    private final String pqcApplet = System.getProperty("user.home") + "\\Desktop\PQC-project\Applet-RAM-benchmark\applet\\";

    private String selectedSmartCardType;

    public UserInputHandler()
    {
        boolean connect = false;
        while (!connect)
        {
            try
            {
                TerminalFactory factory = TerminalFactory.getDefault();
                List<CardTerminal> readers = factory.terminals().list();
                Map<Integer,CardTerminal> readerMap = new HashMap<>();
                System.out.println("Choose a reader:");
                int i = 0;
                for (CardTerminal reader : readers)
                {
                    if (reader.isCardPresent())
                    {
                        i++;
                        readerMap.put(i, reader);
                        System.out.println("\t" + i + ": " + reader.getName());
                    }
                }
                if (readerMap.isEmpty())
                {
                    System.out.println("No readers or cards attached.");
                    return;
                }
                Scanner scanner = new Scanner(System.in);
                String chosenReaderInput = scanner.nextLine();
                if (!readerMap.containsKey(Integer.parseInt(chosenReaderInput)))
                {
                    System.out.println("Reader not available.");
                    continue;
                }
                CardTerminal chosenReader = readerMap.get(Integer.parseInt(chosenReaderInput));
                System.out.println("Select smart card version 1,2 or 3...");
                Scanner smartCardType = new Scanner(System.in);
                selectedSmartCardType = smartCardType.nextLine();
                SmartCard smartCard = null;
                if (selectedSmartCardType.equals("1")) smartCard = new SmartCardVersion1Factory().create(chosenReader);
                if (selectedSmartCardType.equals("2")) smartCard = new SmartCardVersion2Factory().create(chosenReader);
                if (selectedSmartCardType.equals("3")) smartCard = new SmartCardVersion3Factory().create(chosenReader);
                if (smartCard == null)
                {
                    System.out.println("Reader not available.");
                    continue;
                }
                connect = true;
                this.awaitUserInput(smartCard);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    private void awaitUserInput(SmartCard smartCard)
    {
        try
        {
            for (;;)
            {
                Scanner scanner = new Scanner(System.in);
                String command = scanner.nextLine();
                List<String> arguments = Arrays.asList(command.split(" "));
                if (arguments.get(0).equals("cap"))
                {
                    String cap = "RAMtest";
                    if (arguments.size()>1)
                    {
                        cap = arguments.get(1);
                    }
                    CAPFile capFile = CAPFile.fromStream(Files.newInputStream(new File(System.getProperty("user.home") + "\\Desktop\\PQC-project\\Applet-RAM-benchmark\\applet\\RAMtest3\\" + cap + ".cap").toPath()));
                    print(capFile.getCode());
                    capFile.dump(System.out);
                }
                if (arguments.get(0).equals("cm")) smartCard.selectCardManager();
                if (arguments.get(0).equals("pqc")) smartCard.selectPQCApplet();
                if (arguments.get(0).equals("ram")) smartCard.selectRAMApplet();
                if (arguments.get(0).equals("select"))
                {
                    if (arguments.get(1).equals("cm")) smartCard.selectCardManager();
                    if (arguments.get(1).equals("pqc")) smartCard.selectPQCApplet();
                    if (arguments.get(1).equals("ram")) smartCard.selectRAMApplet();
                }
                if (arguments.get(0).equals("apdu"))
                {
                    smartCard.customAPDU(arguments.get(1));
                }
                if (arguments.get(0).equals("secure"))
                {
                    smartCard.openSecureChannel(ChannelType.MAC);
                }
                if (arguments.get(0).equals("test"))
                {
                    smartCard.test();
                }
                if (arguments.get(0).equals("applets"))
                {
                    Applet isd = smartCard.getISD().get(0);
                    List<Applet> applets = smartCard.getApplets();
                    List<Applet> packages = smartCard.getPackages();

                    System.out.println();
                    System.out.println(isd.getType().toString());
                    System.out.print("  AID: ");print(isd.getAID());
                    System.out.println("  LifeCycle: " + isd.getLifeCycle().toString());
                    System.out.print("  Privileges: ");print(isd.getPrivileges());
                    System.out.println("    " + isd.getPrivilegesString());

                    System.out.println();
                    for (Applet applet : applets)
                    {
                        System.out.println(applet.getType().toString());
                        System.out.print("  AID: ");print(applet.getAID());
                        System.out.println("  Lifecycle: " + applet.getLifeCycle().toString());
                        System.out.print("  Version: ");print(applet.getVersion());
                        System.out.print("  Privileges: ");print(applet.getPrivileges());
                        System.out.println("    " + applet.getPrivilegesString());
                    }

                    System.out.println();
                    for (Applet pkg : packages)
                    {
                        System.out.println(pkg.getType().toString());
                        System.out.print("  AID: ");print(pkg.getAID());
                        System.out.println("  Lifecycle: " + pkg.getLifeCycle().toString());
                        System.out.println("    " + pkg.getPrivilegesString());
                    }
                }
                if (arguments.get(0).equals("install"))
                {
                    if (arguments.size() < 2)
                    {
                        System.out.println("Use ram/pqc parameter");
                        continue;
                    }
                    if (arguments.get(1).equals("ram")) smartCard.installApplet(appletLocation + "RAMtest" + selectedSmartCardType + "\\RAMtest.cap");
                    if (arguments.get(1).equals("pqc")) smartCard.installApplet(appletLocation);
                }
                if (arguments.get(0).equals("uninstall"))
                {
                    if (arguments.size() < 2)
                    {
                        System.out.println("Use ram/pqc parameter");
                        continue;
                    }
                    if (arguments.get(1).equals("ram")) smartCard.uninstallApplet(appletLocation + "RAMtest" + selectedSmartCardType + "\\RAMtest.cap");
                    if (arguments.get(1).equals("pqc")) smartCard.uninstallApplet(appletLocation);
                }
                if (arguments.get(0).equals("update"))
                {
                    if (arguments.size() < 2)
                    {
                        System.out.println("Use ram/pqc parameter");
                        continue;
                    }
                    if (arguments.get(1).equals("ram"))
                    {
                        smartCard.uninstallApplet(appletLocation + "RAMtest" + selectedSmartCardType + "\\RAMtest.cap");
                        smartCard.installApplet(appletLocation + "RAMtest" + selectedSmartCardType + "\\RAMtest.cap");
                    }
                    if (arguments.get(1).equals("pqc"))
                    {
                        smartCard.uninstallApplet(appletLocation);
                        smartCard.installApplet(appletLocation);
                    }
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    protected void print(byte[] data)
    {
        StringBuilder sb = new StringBuilder();
        for (byte b : data)
        {
            sb.append(String.format("%02X ", b));
        }
        System.out.print(sb);
        System.out.println();
    }
}