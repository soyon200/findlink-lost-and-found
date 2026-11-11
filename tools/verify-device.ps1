<#
    ডিভাইসে FindLink-এর প্রতিটি স্ক্রিনের স্ক্রিনশট নিয়ে ডিজাইন যাচাই করার স্ক্রিপ্ট।

    ব্যবহার:
        .\tools\verify-device.ps1

    শর্ত:
      * ফোন USB-তে যুক্ত, ডিবাগিং চালু এবং স্ক্রিন আনলক করা থাকতে হবে।
      * debug বিল্ড ইনস্টল করা থাকতে হবে (app/src/debug/AndroidManifest.xml অ্যাক্টিভিটিগুলো export করে,
        তাই adb থেকে সরাসরি খোলা যায়)।

    আউটপুট: screens\verify\*.png
#>

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$apk = Join-Path $root "app\build\outputs\apk\debug\app-debug.apk"
$outDir = Join-Path $root "screens\verify"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

function Assert-Device {
    $devices = adb devices | Select-String "device$"
    if (-not $devices) {
        throw "কোনো ডিভাইস পাওয়া যায়নি। USB সংযোগ ও USB debugging চালু করুন।"
    }
}

function Capture($name) {
    adb shell screencap -p /sdcard/_findlink_shot.png | Out-Null
    adb pull /sdcard/_findlink_shot.png (Join-Path $outDir "$name.png") 2>$null | Out-Null
    Write-Host "  saved $name.png"
}

# $extra একটি সম্পূর্ণ স্ট্রিং হিসেবে পাঠানো হয়, যাতে স্পেসসহ টেক্সট (যেমন item title) ঠিকমতো পৌঁছায়
function Show-Screen($name, $component, [string]$extra = "", [int]$waitSeconds = 2) {
    Write-Host "Capturing $name ..."
    adb shell am force-stop com.example.findlink | Out-Null
    $cmd = "am start -n $component $extra".Trim()
    adb shell $cmd | Out-Null
    Start-Sleep -Seconds $waitSeconds
    Capture $name
}

Assert-Device
Write-Host "Installing $apk"
adb install -r $apk | Out-Null

# স্ক্রিন যেন ঘুমিয়ে না পড়ে
adb shell svc power stayon usb | Out-Null

$pkg = "com.example.findlink"

Show-Screen "01_splash"   "$pkg/.activities.SplashActivity" "" 1
Show-Screen "02_home"     "$pkg/.MainActivity" "" 3
Show-Screen "03_search"   "$pkg/.MainActivity" "--ei startTab 1" 3
Show-Screen "04_add_post" "$pkg/.MainActivity" "--ei startTab 2" 3
Show-Screen "05_profile"  "$pkg/.MainActivity" "--ei startTab 3" 3
Show-Screen "06_login"    "$pkg/.activities.LoginActivity" "" 2
Show-Screen "07_register" "$pkg/.activities.RegisterActivity" "" 2
Show-Screen "08_details"  "$pkg/.activities.ItemDetailsActivity" "--es title 'Black leather wallet' --es category 'Accessories' --es location 'Central Library, 2nd floor' --es description 'Black leather wallet with a blue stripe on the back.' --es reportType 'LOST' --es itemId 'demo-item' --es userId 'demo-user' --es phone '+8801712345678'" 2
Show-Screen "09_claim"    "$pkg/.activities.ClaimActivity" "--es itemId 'demo-item'" 2
Show-Screen "10_contact"  "$pkg/.activities.ContactActivity" "--es itemId 'demo-item'" 2
Show-Screen "11_inbox"    "$pkg/.activities.InboxActivity" "" 3

Write-Host ""
Write-Host "সব স্ক্রিনশট এখানে সংরক্ষিত হয়েছে: $outDir"
