#!/usr/bin/env python3
import os

SCREENS_DIR = "app/src/main/java/com/toxicplants/database/ui/screens"

def fix_reagents():
    path = os.path.join(SCREENS_DIR, "ChemicalReagentsScreen.kt")
    if not os.path.exists(path): return
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()
    content = content.replace('color = Color(0xFFB71C1C)', 'color = MaterialTheme.colorScheme.onErrorContainer')
    with open(path, "w", encoding="utf-8") as f:
        f.write(content)
    print("✅ Corregido contraste en ChemicalReagentsScreen.kt")

def fix_extractions():
    path = os.path.join(SCREENS_DIR, "ChemicalExtractionMethodsScreen.kt")
    if not os.path.exists(path): return
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()
    content = content.replace('color = Color(0xFF6D4C00)', 'color = MaterialTheme.colorScheme.onTertiaryContainer')
    with open(path, "w", encoding="utf-8") as f:
        f.write(content)
    print("✅ Corregido contraste en ChemicalExtractionMethodsScreen.kt")

def fix_compound_detail():
    path = os.path.join(SCREENS_DIR, "CompoundDetailScreen.kt")
    if not os.path.exists(path): return
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()
    content = content.replace('color = Color(0xFF6D4C00)', 'color = MaterialTheme.colorScheme.onTertiaryContainer')
    with open(path, "w", encoding="utf-8") as f:
        f.write(content)
    print("✅ Corregido contraste en CompoundDetailScreen.kt")

def fix_compound_interactions():
    path = os.path.join(SCREENS_DIR, "CompoundInteractionsScreen.kt")
    if not os.path.exists(path): return
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()
    content = content.replace('color = Color(0xFF6D4C00)', 'color = MaterialTheme.colorScheme.onTertiaryContainer')
    with open(path, "w", encoding="utf-8") as f:
        f.write(content)
    print("✅ Corregido contraste en CompoundInteractionsScreen.kt")

def fix_emergency():
    path = os.path.join(SCREENS_DIR, "EmergencyScreen.kt")
    if not os.path.exists(path): return
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()
    content = content.replace('color = Color(0xFF6D4C00)', 'color = MaterialTheme.colorScheme.onTertiaryContainer')
    with open(path, "w", encoding="utf-8") as f:
        f.write(content)
    print("✅ Corregido contraste en EmergencyScreen.kt")

def fix_risk_calculator():
    path = os.path.join(SCREENS_DIR, "RiskCalculatorScreen.kt")
    if not os.path.exists(path): return
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()
    content = content.replace('containerColor = color.copy(alpha = 0.10f)', 'containerColor = MaterialTheme.colorScheme.surfaceVariant')
    content = content.replace('Text(r.summary, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium)',
                              'Text(r.summary, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)')
    content = content.replace('Text(r.advice, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 19.sp)',
                              'Text(r.advice, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 19.sp)')
    with open(path, "w", encoding="utf-8") as f:
        f.write(content)
    print("✅ Corregido contraste en RiskCalculatorScreen.kt")

def main():
    fix_reagents()
    fix_extractions()
    fix_compound_detail()
    fix_compound_interactions()
    fix_emergency()
    fix_risk_calculator()

if __name__ == "__main__":
    main()
