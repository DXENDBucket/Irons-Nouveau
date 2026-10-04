package dev.ironsnouveau.casting;

/** Applied to native entities by a conditional mixin; ordinary Iron projectiles have no session. */
public interface NativeCastCarrier {
    CastSession ironsNouveau$session();
    void ironsNouveau$session(CastSession session);
    boolean ironsNouveau$arsDriven();
    void ironsNouveau$arsDriven(boolean value);
}
