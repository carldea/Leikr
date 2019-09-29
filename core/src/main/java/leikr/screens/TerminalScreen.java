/*
 * Copyright 2019 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package leikr.screens;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import leikr.ExportTool;
import leikr.GameRuntime;
import leikr.loaders.EngineLoader;
import org.mini2Dx.core.Graphics;
import org.mini2Dx.core.Mdx;
import org.mini2Dx.core.files.FileHandle;
import org.mini2Dx.core.game.GameContainer;
import org.mini2Dx.core.graphics.Colors;
import org.mini2Dx.core.graphics.viewport.FitViewport;
import org.mini2Dx.core.screen.BasicGameScreen;
import org.mini2Dx.core.screen.GameScreen;
import org.mini2Dx.core.screen.ScreenManager;
import org.mini2Dx.core.screen.Transition;
import org.mini2Dx.gdx.Input.Keys;
import org.mini2Dx.gdx.InputProcessor;

/**
 *
 * @author Torbuntu
 */
public class TerminalScreen extends BasicGameScreen implements InputProcessor {

    public static int ID = 7;

    boolean RUN_PROGRAM = false;

    String out;

    FitViewport viewport;

    String prompt = "";
    String historyText = "";

    int blink = 0;

    public TerminalScreen() {
        viewport = new FitViewport(GameRuntime.WIDTH, GameRuntime.HEIGHT);
    }

    private void setProcessor() {
        Mdx.input.setInputProcessor(this);
    }

    private String runLs() {
        try {
            out = "";
            for (FileHandle f : Mdx.files.local("Programs").list()) {
                out += f.nameWithoutExtension() + "\n";
            }
            return out;
        } catch (IOException ex) {
            Logger.getLogger(EngineLoader.class.getName()).log(Level.WARNING, null, ex);
            return "Failed to execute command ( ls )";
        }
    }

    @Override
    public void initialise(GameContainer gc) {

    }

    @Override
    public void preTransitionIn(Transition trns) {
        prompt = "";
        out = runLs();
        if (GameRuntime.GAME_NAME.length() < 2) {
            historyText = "No program loaded.";
        } else {
            historyText = "Loaded program: `" + GameRuntime.GAME_NAME + "`";
        }
        setProcessor();
    }

    @Override
    public void update(GameContainer gc, ScreenManager<? extends GameScreen> sm, float delta) {
        if (RUN_PROGRAM) {
            RUN_PROGRAM = false;
            historyText = "";
            sm.enterGameScreen(LoadScreen.ID, null, null);
        }
        blink++;
        if (blink > 60) {
            blink = 0;
        }
    }

    @Override
    public void render(GameContainer gc, Graphics g) {
        viewport.apply(g);
        g.setColor(Colors.GREEN());

        g.drawString(historyText, 0, 0, 240);
        g.setColor(Colors.BLACK());
        g.fillRect(0, 152, 240, 160);
        g.setColor(Colors.GREEN());
        g.drawString(">" + prompt + ((blink > 30) ? (char) 173 : ""), 0, 152, 240);
    }

    @Override
    public int getId() {
        return ID;
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Keys.ESCAPE) {
            Mdx.platformUtils.exit(true);
        }
        if (keycode == Keys.ENTER) {
            historyText = processCommand() + "\n\n";
            prompt = "";
            return true;
        }
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char c) {
        if ((int) c >= 32 && (int) c <= 126) {
            prompt = prompt + c;
            return true;
        }
        if ((int) c == 8 && prompt.length() > 0) {
            prompt = prompt.substring(0, prompt.length() - 1);
        }
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(int amount) {
        return false;
    }

    private String processCommand() {
        String[] command = prompt.split(" ");
        switch (command[0]) {
            case "clean": {
                try {
                    Mdx.files.local("Packages/").deleteDirectory();
                    return "Package directory cleaned.";
                } catch (IOException ex) {
                    Logger.getLogger(TerminalScreen.class.getName()).log(Level.SEVERE, null, ex);
                    return "Failed to clean package directory. Please check logs.";
                }
            }

            case "clear":
                return "";
            case "exit":
                Mdx.platformUtils.exit(true);
                return "Goodbye";
            case "export":
                return ExportTool.export(command[1]);
            case "exportAll":
                return ExportTool.exportAll();
            case "help":
                if (command.length > 1) {
                    switch (command[1]) {
                        case "exit":
                            return ">exit \nExits the Leikr Game system.";
                        case "clear":
                            return ">clear \nClears the terminal screen text.";
                        case "help":
                            return ">help [option] \nDisplays the help options to the screen or info about a command.";
                        case "load":
                            return ">load [option] \nLoads the program by name. Does not check if program exists.";
                        case "ls":
                            return ">ls \nDisplays the contents of the Programs directory.";
                        case "run":
                            return ">run [arg] \nLoads and Runs a program given a title.";
                        default:
                            return "No help for unknown command: ( " + command[1] + " )";
                    }
                }
                return "Commands: exit, clear, help, load, ls, run";
            case "install":
                return ExportTool.importProject(command[1]);
            case "load":
                if (command.length < 2) {
                    return "Missing required param [program-title]";
                }
                if (!out.contains(command[1])) {
                    return "Program [" + command[1] + "] does not exist in Programs directory.";
                }
                GameRuntime.GAME_NAME = command[1];
                GameRuntime.setProgramPath("Programs/" + command[1]);
                return "Loaded program: `" + GameRuntime.GAME_NAME + "`";
            case "ls":
                return runLs();
            case "run":
                if (command.length == 1 && GameRuntime.GAME_NAME.length() > 2) {
                    RUN_PROGRAM = true;
                    return "loading...";
                }
                try {
                    if (!out.contains(command[1])) {
                        return "Program [" + command[1] + "] does not exist in Programs directory.";
                    }
                    GameRuntime.GAME_NAME = command[1];
                    GameRuntime.setProgramPath("Programs/" + command[1]);
                    RUN_PROGRAM = true;
                    return "loading...";
                } catch (Exception ex) {
                    Logger.getLogger(EngineLoader.class.getName()).log(Level.WARNING, null, ex);
                    return "Failed to run program with name ( " + command[1] + " )";
                }
            default:
                return "Uknown command: ( " + prompt + " )";
        }
    }

}