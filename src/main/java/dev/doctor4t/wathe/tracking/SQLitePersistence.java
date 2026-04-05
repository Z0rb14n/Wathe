package dev.doctor4t.wathe.tracking;

import dev.doctor4t.wathe.Wathe;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SQLitePersistence extends Persistence {
    private static final String FILE_PATH = "wathe_data.sqlite";
    private Connection conn;

    // based on https://github.com/Athlaeos/ValhallaMMO/blob/master/core/src/main/java/me/athlaeos/valhallammo/persistence/implementations/SQLite.java#L20
    @Override
    public Connection getConnection() {
        File dataFolder = FabricLoader.getInstance().getGameDir().resolve(FILE_PATH).toFile();
        if (!dataFolder.exists()){
            try {
                if (dataFolder.createNewFile()) Wathe.LOGGER.info("New {} file created!", FILE_PATH);
            } catch (IOException e) {
                Wathe.LOGGER.error("Could not create SQLite DB file {}", FILE_PATH, e);
            }
        }

        try {
            if (conn != null && !conn.isClosed()){
                return conn;
            }
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:" + dataFolder);
            Wathe.LOGGER.debug("SQLite connection created! Deleting this file will reset everyone's progress, so back this file up or ignore it in case you want to delete/reset the configs.");
            return conn;
        } catch (SQLException ex) {
            Wathe.LOGGER.error("SQLite exception on initialize", ex);
        } catch (ClassNotFoundException ex) {
            Wathe.LOGGER.error("You do not have the SQLite JDBC library on your server", ex);
        }
        return null;
    }
}
