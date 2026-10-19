package studio.cbk.zenvoryx;

/** Austauschbarer Baustein: Regeln heute, später vielleicht eine eigene KI. */
public interface AiBackend {
    String handle(String command, Actions actions);
}
