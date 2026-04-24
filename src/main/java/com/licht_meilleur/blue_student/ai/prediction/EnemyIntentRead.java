package com.licht_meilleur.blue_student.ai.prediction;

public class EnemyIntentRead {

    // --- 脅威評価 ---
    public double meleeThreat;    // 近接圧
    public double rangedThreat;   // 射撃圧
    public double chargeThreat;   // 溜め・大技っぽさ

    // --- 行動傾向 ---
    public boolean closingIn;     // 接近中
    public boolean backingOff;    // 後退中
    public boolean holdingStill;  // 停止中

    // --- 攻めやすさ ---
    public double opening;        // 隙

    // --- デバッグ ---
    public double distance;
    public double deltaDistance;
    public double targetSpeed;
    public boolean canSee;
    public boolean targetFacingSelf;
    public boolean targetingSelf;
    public int hurtTime;
    public int stillTicks;

    public String toDebugString() {
        return "Intent[" +
                "d=" + fmt(distance) +
                ", Δ=" + fmt(deltaDistance) +
                ", spd=" + fmt(targetSpeed) +
                ", melee=" + fmt(meleeThreat) +
                ", ranged=" + fmt(rangedThreat) +
                ", charge=" + fmt(chargeThreat) +
                ", open=" + fmt(opening) +
                ", close=" + closingIn +
                ", back=" + backingOff +
                ", still=" + holdingStill +
                "]";
    }

    private static String fmt(double v) {
        return String.format(java.util.Locale.ROOT, "%.2f", v);
    }
}