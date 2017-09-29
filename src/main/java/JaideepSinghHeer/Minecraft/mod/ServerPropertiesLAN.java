package JaideepSinghHeer.Minecraft.mod;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.io.Files;
import com.typesafe.config.ConfigIncluderClasspath;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.server.management.UserListWhitelist;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.*;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * We cannot use the {@link Mod} annotation as the mod is already instantiated
 * via the {@link IFMLLoadingPlugin} interface as a CoreMod.
 *
 * We need it to be a CoreMod so that we can Edit the ByteCode
 * using the {@link net.minecraft.launchwrapper.IClassTransformer} interface.
 *
 * We Edit the ByteCode of the {@link net.minecraft.util.HttpUtil} class to return our specified Port for LAN connections.
 * @see net.minecraft.util.HttpUtil for the getSuitableLanPort() method which returns a LAN port.
 *
 */

//@Mod(modid = ServerPropertiesLAN.MODID,name=ServerPropertiesLAN.MODNAME, version = ServerPropertiesLAN.VERSION,clientSideOnly = true,acceptableRemoteVersions = "*",useMetadata = true)
@SideOnly(Side.CLIENT)
public class ServerPropertiesLAN extends DummyModContainer implements IFMLLoadingPlugin
{
    public int port=-1;
    private static boolean whiteListFirstRun;
    private static MinecraftServer server;
 
    public static final String MODID = "serverpropertieslan";
    public static final String MODNAME = "Server Properties LAN";
    public static final String VERSION = "2.5";
    private static File configDirectory;

    // This Class manages all the File IO.
    private PropertyManagerClient ServerProperties = null;
    // Logger to get output in The Log.
    private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

    /**
     * We don't want to do a lot of work so we extend {@link DummyModContainer}.
     * As it contains a {@link ModMetadata} object,
     * it must be initialised.
     *
     */
    public ServerPropertiesLAN()
    {
        super(new ModMetadata());
        whiteListFirstRun = false;
        System.out.println("-=-=-=-=-=-=-=ServerPropertiesLAN-Constructed=-=-=-=-=-=-=-");
        // static instance to always get the correct object.
        instance = this;
        // Mod Metadata defined in DummyModContainer received by func. getMetadata()
        ModMetadata md = getMetadata();
        md.autogenerated = false;
        md.useDependencyInformation = false;
        md.modId=MODID;
        md.version=VERSION;
        md.name=MODNAME;
        md.authorList = Lists.newArrayList("Jaideep Singh Heer");
        md.description = "MeoW.!";
        md.credits = "by Jaideep Singh Heer";
        md.logoFile = "logo.png";
        md.screenshots = new String[]{"scr1.jpg", "Untitled.jpg", "logo2.png"};
        md.url = "https://minecraft.curseforge.com/projects/server-properties-for-lan";
    }

    /**
     * Recieves the FMLPreinitialization Event to set the Mod's config directory.
     * @param e
     */
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e)
    {
        configDirectory = e.getModConfigurationDirectory();
    }

    /**
     * We cannot use {@link net.minecraftforge.fml.common.Mod.EventHandler} as that is a part of the {@link Mod} annotation
     * and hence requires a Class to be annotated with the {@link Mod} annotation which cannot be done for CoreMods.<See Above>
     *
     * Therefore we must register this class to the {@link EventBus} provided to it for being the {@link ModContainer}.
     *
     */
    @Override
    public boolean registerBus(EventBus bus, LoadController controller)
    {
        //System.out.println("-=-=-=-=-=-=-=EventBusRegistered=-=-=-=-=-=-=-=-");
        bus.register(this);
        return true;
    }

    /**
     * The static instance of this Class to be accessed as a Mod.
     * Forge automatically instantiates an Object for us
     * and we assign that to this object(called instance) in the constructor.
     */
    public static ServerPropertiesLAN instance;

    /**
     * This function is subscribed to the {@link EventBus} via the {@link Subscribe} annotation.
     * The type of event({@link net.minecraftforge.fml.common.eventhandler.Event}) to be subscribed is judged from the prototype.
     * This function gets the {@link net.minecraft.server.MinecraftServer} from the event
     * and gets the world save directory using the {@link DimensionManager}.
     *
     * It then uses the {@link PropertyManagerClient} Class to save/load data from the server.properties file
     * and sets the attributes of the {@link net.minecraft.server.MinecraftServer} via its functions.
     *
     */
    @Subscribe
    public void onServerStarting(FMLServerStartingEvent event) {
        System.out.println("========================>> Server Starting !");
        // Define the config files.
        File local = new File(DimensionManager.getCurrentSaveRootDirectory()+File.separator+"server.properties");
        File global = new File(Minecraft.getMinecraft().mcDataDir+File.separator+"config"+File.separator+"serverGlobalConfig.properties");

        if(local.exists()) {
            ServerProperties = new PropertyManagerClient(local);
            if(!ServerProperties.getBooleanProperty("overrideGlobalDefaults", false)) {
                ServerProperties = new PropertyManagerClient(global);
                LOGGER.info("Using Global Server Properties !");
            }
        }
        else{
            ServerProperties = new PropertyManagerClient(global);}

            LOGGER.info("Using file : "+ServerProperties.getPropertiesFile().getPath());
        ServerProperties.comment = "Minecraft Server Properties for LAN.";
        ServerProperties.comment += System.getProperty("line.separator")+"For default behaviour :-";
        ServerProperties.comment += System.getProperty("line.separator")+"set max-view-distance=0";
        ServerProperties.comment += System.getProperty("line.separator")+"You can also delete this(or any properties) file to get it regenerated with default values.";

        port = ServerProperties.getIntProperty("port", 25565);
        server = event.getServer();
        server.setOnlineMode(ServerProperties.getBooleanProperty("online-mode", true));
        server.setCanSpawnAnimals(ServerProperties.getBooleanProperty("spawn-animals", true));
        server.setCanSpawnNPCs(ServerProperties.getBooleanProperty("spawn-npcs", true));
        server.setAllowPvp(ServerProperties.getBooleanProperty("pvp", true));
        server.setAllowFlight(ServerProperties.getBooleanProperty("allow-flight", false));
        server.setResourcePack(ServerProperties.getStringProperty("resource-pack-sha1", ""), this.loadResourcePackSHA());
        server.setMOTD(ServerProperties.getStringProperty("motd", "<! "+server.getServerOwner() + "'s " + server.worlds[0].getWorldInfo().getWorldName()+" ON LAN !>"));
        server.setPlayerIdleTimeout(ServerProperties.getIntProperty("player-idle-timeout", 0));
        server.setBuildLimit(ServerProperties.getIntProperty("max-build-height", 256));

        // Print data to the console
        LOGGER.info("Server Data :- ");
        LOGGER.info("online-mode = "+server.isServerInOnlineMode());
        LOGGER.info("spawn-animals = "+server.getCanSpawnAnimals());
        LOGGER.info("spawn-npcs = "+server.getCanSpawnNPCs());
        LOGGER.info("pvp = "+server.isPVPEnabled());
        LOGGER.info("allow-flight = "+server.isFlightAllowed());
        LOGGER.info("player-idle-timeout = "+server.getMaxPlayerIdleMinutes());
        LOGGER.info("max-build-height = "+server.getBuildLimit());
        LOGGER.info("resource-pack-sha1 = "+server.getResourcePackHash());
        LOGGER.info("motd = "+server.getMOTD());

        // Get the PlayerList Settings Object
        PlayerList customPlayerList =  server.getPlayerList();

        // REFLECTION !!
        // NOTE : We need to make sure it works after obfuscation and so we use the ReflectionHelper class
        // which basically lets us specify many possible names for the field ...!
        try {
            // Set MaxPlayers
            Field field = ReflectionHelper.findField(PlayerList.class,"maxPlayers","field_72405_c");
            field.setAccessible(true);
            field.set(customPlayerList, ServerProperties.getIntProperty("max-players", 10));
            LOGGER.info("Max Players = "+customPlayerList.getMaxPlayers());

            // Set MaxViewDistance
            Field dist = ReflectionHelper.findField(PlayerList.class,"viewDistance","field_72402_d");
            dist.setAccessible(true);
            int d = ServerProperties.getIntProperty("max-view-distance", 0);
            if(d>0){dist.set(customPlayerList, d);LOGGER.info("Max view distance = "+d);}
            else LOGGER.info("Using default view distance algorithm.");

            if (ServerProperties.getBooleanProperty("white-list", false))
            {
                LOGGER.warn("=====>>WARNING whitelisting enabled...! Make sure at least one user entry is in the whitelist.json file !");
                File whitelistjson = new File(DimensionManager.getCurrentSaveRootDirectory() + File.separator+"whitelist.json");
                UserListWhitelist whitelist = new UserListWhitelist(whitelistjson);
                if(!whitelistjson.exists()) {
                    whitelistjson.createNewFile();
                    whitelist.writeChanges();
                    whiteListFirstRun = true;
                    // Set WhiteList
                    field = ReflectionHelper.findField(PlayerList.class,"whiteListedPlayers","field_72411_j");
                    field.setAccessible(true);
                    field.set(customPlayerList, whitelist);
                }
                else {
                    // Not In First Run.
                    customPlayerList.setWhiteListEnabled(true);
                }
            }
            server.setPlayerList(customPlayerList);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        if(!local.exists())
        {
            try {
                Files.copy(global,local);
                ServerProperties = new PropertyManagerClient(local);
                ServerProperties.comment += System.getProperty("line.separator")+"overrideGlobalDefaults :"+System.getProperty("line.separator")+"\tspecify weather to use this file to override the global settings in the file \""+global.getAbsolutePath()+"\"";
                ServerProperties.getBooleanProperty("overrideGlobalDefaults", false);
                ServerProperties.saveProperties();
            } catch (IOException e) {
                LOGGER.error("Oops..! Couldn't copy to local server config file. Please manually copy the global server config file to your world save directory.");
                e.printStackTrace();
            }
        }
    }

    /**
     * This doesn't work sadly ! :(
     * The Event is never fired on the client side it seems :(
     * */
    @Subscribe
    public void onPlayerConnect(EntityJoinWorldEvent e) {
        System.out.println("==================>> Player Connect");
        if (whiteListFirstRun) {
            System.out.println("--------------ID="+e.getEntity().getUniqueID()+"\tNAME="+e.getEntity().getName());
            server.getPlayerList().addWhitelistedPlayer(e.getEntity().getServer().getPlayerList().getOnlinePlayerProfiles()[0]);
            server.getPlayerList().setWhiteListEnabled(true);
            try {
                server.getPlayerList().getWhitelistedPlayers().writeChanges();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    /**
     * These functions are a part of the {@link IFMLLoadingPlugin} interface.
     * @see IFMLLoadingPlugin for details.
     */
    @Override
    public String[] getASMTransformerClass() {
        return new String[]{SPLANtransformerPort.class.getCanonicalName()};
    }

    @Override
    public String getModContainerClass() {
        return ServerPropertiesLAN.class.getCanonicalName();
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data){
        ;
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

    @Override
    public Object getMod()
    {
        return instance;
    }


    /**
     * This function checks the current ResoursePackSHA's validity
     * and returns the final ResoursePackSHA values of the server.
     */
    private String loadResourcePackSHA()
    {
        if (ServerProperties.hasProperty("resource-pack-hash"))
        {
            if (ServerProperties.hasProperty("resource-pack-sha1"))
            {
                LOGGER.warn("resource-pack-hash is deprecated and found along side resource-pack-sha1. resource-pack-hash will be ignored.");
            }
            else
            {
                LOGGER.warn("resource-pack-hash is deprecated. Please use resource-pack-sha1 instead.");
                ServerProperties.getStringProperty("resource-pack-sha1", ServerProperties.getStringProperty("resource-pack-hash", ""));
                ServerProperties.removeProperty("resource-pack-hash");
            }
        }

        String s = ServerProperties.getStringProperty("resource-pack-sha1", "");

        if (!s.isEmpty() && !Pattern.compile("^[a-fA-F0-9]{40}$").matcher(s).matches())
        {
            LOGGER.warn("Invalid sha1 for ressource-pack-sha1");
        }

        if (!ServerProperties.getStringProperty("resource-pack", "").isEmpty() && s.isEmpty())
        {
            LOGGER.warn("You specified a resource pack without providing a sha1 hash. Pack will be updated on the client only if you change the name of the pack.");
        }

        return s;
    }


    }

