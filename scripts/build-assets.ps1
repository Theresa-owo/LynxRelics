param([string]$Modules)
$ErrorActionPreference = 'Stop'
$taskModuleArgs = if ($Modules) { @('--modules', $Modules) } else { @() }
Push-Location (Split-Path -Parent $PSScriptRoot)
try {
    & node scripts/generate-enchantments.mjs
    if ($LASTEXITCODE -ne 0) { throw 'Enchantment generation failed.' }
    & node scripts/generate-recipes.mjs
    if ($LASTEXITCODE -ne 0) { throw 'Recipe generation failed.' }
    & node scripts/generate-accessories.mjs --color @taskModuleArgs
    if ($LASTEXITCODE -ne 0) { throw 'Accessory generation failed.' }
    & node scripts/generate-mob-models.mjs --color
    if ($LASTEXITCODE -ne 0) { throw 'Mob model generation failed.' }
    # generate-assets also bakes final material UVs and the symmetric bow rig.
    & node scripts/generate-assets.mjs @taskModuleArgs
    if ($LASTEXITCODE -ne 0) { throw 'Runtime asset generation failed.' }
    & node scripts/compile-icons.mjs @taskModuleArgs
    if ($LASTEXITCODE -ne 0) { throw 'Icon compilation failed.' }
} finally { Pop-Location }
