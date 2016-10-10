/*
 * Copyright 2016 Johannes Donath <johannesd@torchmind.com>
 * and other copyright owners as documented in the project's IP log.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rocks.spud.minecraft.mineclock;

import com.google.inject.Guice;
import com.google.inject.Injector;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import javax.annotation.Nonnull;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import rocks.spud.minecraft.mineclock.inject.FXMLProvider;

/**
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class MineClockApplication extends Application {
    private static final String[] TOOLS_PATH_FORMATS = new String[]{"../lib/tools.jar", "../Classes/classes.jar", "lib/tools.jar", "Classes/classes.jar"};
    private Injector injector;

    /**
     * Augments the bootstrap class loader using some reflection magic in order to dynamically
     * append the JDK's tools.jar to the classpath when available.
     */
    private static void augmentBootstrapClassLoader() {
        Map<String, String> environment = System.getenv();
        Path jdkPath = null;
        String toolsFormat = null;

        for (String toolPathFormat : TOOLS_PATH_FORMATS) {
            jdkPath = Paths.get(System.getProperty("java.home")).toAbsolutePath();
            toolsFormat = toolPathFormat;
            System.out.println("Checking " + jdkPath.toString() + " for valid JDK libraries");

            if (Files.notExists(jdkPath.resolve(toolPathFormat)) && environment.containsKey("JAVA_HOME")) {
                jdkPath = Paths.get(environment.get("JAVA_HOME")).toAbsolutePath();
                toolsFormat = toolPathFormat;
                System.out.println("Checking " + jdkPath.toString() + " for valid JDK libraries");

                if (Files.exists(jdkPath.resolve(toolPathFormat))) {
                    break;
                }
            } else {
                break;
            }
        }

        if (jdkPath != null && toolsFormat != null && Files.exists(jdkPath.resolve(toolsFormat))) {
            System.out.println("Using JDK path: " + jdkPath.toString());

            try {
                URLClassLoader classLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();

                // append tools.jar to class loader
                Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                method.setAccessible(true);
                method.invoke(classLoader, jdkPath.resolve(toolsFormat).toUri().toURL());

                // integrate natives
                System.setProperty("java.library.path", jdkPath.resolve("../jre/bin").toAbsolutePath().toString());
                System.out.println(jdkPath.resolve("../jre/bin").toAbsolutePath().toString());

                Field fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
                fieldSysPath.setAccessible(true);
                fieldSysPath.set(null, null);
            } catch (IllegalAccessException | InvocationTargetException | MalformedURLException | NoSuchFieldException | NoSuchMethodException ignore) {
            }
        }
    }

    /**
     * Retrieves the path to a systems specific storage directory.
     *
     * @return a storage directory.
     */
    @Nonnull
    public static Path getApplicationDirectory() {
        Path basePath = null;

        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            String applicationDataDirectory = System.getenv("APPDATA");

            if (applicationDataDirectory != null) {
                basePath = Paths.get(applicationDataDirectory, ".mineclock");
            }
        }

        if (basePath == null) {
            basePath = Paths.get(System.getProperty("java.home"), ".mineclock");
        }

        try {
            Files.createDirectories(basePath);
        } catch (IOException ex) {
            throw new RuntimeException("Could not create application storage directory: " + ex.getMessage(), ex);
        }

        return basePath;
    }

    public static void main(String[] arguments) {
        augmentBootstrapClassLoader();
        launch(MineClockApplication.class, arguments);
    }

    /**
     * Displays a dialog which reports an unexpected exception to a user in a way that allows them
     * to easily report it.
     */
    public static void reportError(@Nonnull Throwable throwable) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initModality(Modality.APPLICATION_MODAL);

        alert.setTitle("Application Error");
        alert.setHeaderText("Application Error");
        alert.setContentText("An unexpected condition occurred and the requested action has not completed successfully. This is a bug, please report it as such.");

        // Create expandable Exception.
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        String exceptionText = sw.toString();

        TextArea textArea = new TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(textArea, 0, 1);

        alert.getDialogPane().setExpandableContent(expContent);
        throwable.printStackTrace();
        alert.showAndWait();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(@Nonnull final Stage primaryStage) throws Exception {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            reportError(e);
            System.exit(-1);
        });

        primaryStage.getIcons().add(new Image(this.getClass().getResourceAsStream("/image/application.png")));

        this.injector = Guice.createInjector((b) -> {
            b.bind(MineClockApplication.class).toInstance(this);
            b.bind(Stage.class).toInstance(primaryStage);
            b.bind(FXMLLoader.class).toProvider(FXMLProvider.class);
        });

        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setResizable(false);
        primaryStage.setWidth(960);
        primaryStage.setHeight(540);

        FXMLLoader loader = this.injector.getInstance(FXMLLoader.class);
        Scene scene = new Scene(loader.load(this.getClass().getResourceAsStream("/fxml/MainWindow.fxml")));

        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
