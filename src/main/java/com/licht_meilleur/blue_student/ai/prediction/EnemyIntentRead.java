package com.licht_meilleur.blue_student.ai.prediction;

public class EnemyIntentRead {
    public double meleeThreat;    // 近接の危険
    public double rangedThreat;   // 遠距離の危険
    public double chargeThreat;   // 大技予兆

    public boolean closingIn;     // 接近中
    public boolean backingOff;    // 後退中
    public boolean holdingStill;  // 停止中

    public double opening;        // 隙
}