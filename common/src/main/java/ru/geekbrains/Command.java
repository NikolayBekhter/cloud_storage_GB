package ru.geekbrains;

public enum Command {

    SEND_FILE_COMMAND("file"),
    GET_FILES_LIST_COMMAND("list"),
    GET_FILE_COMMAND("get_File");

    private final String simpleName;

    Command(String simpleName) {
        this.simpleName = simpleName;
    }

    public String getSimpleName() {
        return simpleName;
    }
}
