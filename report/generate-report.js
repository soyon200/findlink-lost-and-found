const fs = require("fs");
const path = require("path");
const {
  Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell, ImageRun,
  Header, Footer, AlignmentType, LevelFormat, HeadingLevel, BorderStyle,
  WidthType, ShadingType, VerticalAlign, PageNumber, PageBreak
} = require("docx");

const OUT = path.join(__dirname, "FindLink-Lost-and-Found-Project-Report.docx");
const SHOT = (p) => path.join(__dirname, "..", "screens", p);

// ---------- helpers ----------
const border = { style: BorderStyle.SINGLE, size: 1, color: "CCCCCC" };
const cellBorders = { top: border, bottom: border, left: border, right: border };

const p = (text, opts = {}) =>
  new Paragraph({
    spacing: { after: 90, line: 262 },
    ...opts,
    children: [new TextRun({ text, ...(opts.run || {}) })]
  });

const bullet = (text, ref = "bullets") =>
  new Paragraph({
    numbering: { reference: ref, level: 0 },
    spacing: { after: 60, line: 262 },
    children: [new TextRun({ text })]
  });

const h1 = (text) =>
  new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun(text)] });

const h2 = (text) =>
  new Paragraph({ heading: HeadingLevel.HEADING_2, children: [new TextRun(text)] });

// Title-page line: centred, serif, spacing-controlled
const coverLine = (text, opts = {}) =>
  new Paragraph({
    alignment: AlignmentType.CENTER,
    spacing: opts.spacing || { after: 60 },
    children: [new TextRun({
      text,
      font: "Times New Roman",
      size: opts.size || 22,
      bold: !!opts.bold,
      italics: !!opts.italics,
      color: "000000"
    })]
  });

function img(file, wPx) {
  const hPx = Math.round((wPx * 2400) / 1080);
  return new ImageRun({
    type: "png",
    data: fs.readFileSync(file),
    transformation: { width: wPx, height: hPx },
    altText: { title: path.basename(file), description: "FindLink app screenshot", name: path.basename(file) }
  });
}

function figure(file, caption, wPx = 150) {
  return [
    new Paragraph({
      alignment: AlignmentType.CENTER,
      spacing: { before: 120, after: 40 },
      children: [img(file, wPx)]
    }),
    new Paragraph({
      alignment: AlignmentType.CENTER,
      spacing: { after: 200 },
      children: [new TextRun({ text: caption, italics: true, size: 18, color: "555555" })]
    })
  ];
}

function twoFigures(file1, cap1, file2, cap2, wPx = 145) {
  const cell = (f, c) =>
    new TableCell({
      borders: { top: { style: BorderStyle.NONE }, bottom: { style: BorderStyle.NONE }, left: { style: BorderStyle.NONE }, right: { style: BorderStyle.NONE } },
      width: { size: 4680, type: WidthType.DXA },
      children: [
        new Paragraph({ alignment: AlignmentType.CENTER, children: [img(f, wPx)] }),
        new Paragraph({
          alignment: AlignmentType.CENTER,
          spacing: { before: 60, after: 120 },
          children: [new TextRun({ text: c, italics: true, size: 17, color: "555555" })]
        })
      ]
    });
  return new Table({
    columnWidths: [4680, 4680],
    margins: { top: 0, bottom: 0, left: 0, right: 0 },
    rows: [new TableRow({ children: [cell(file1, cap1), cell(file2, cap2)] })]
  });
}

function dataTable(headers, rows, widths) {
  const head = new TableRow({
    tableHeader: true,
    children: headers.map((h, i) =>
      new TableCell({
        borders: cellBorders,
        width: { size: widths[i], type: WidthType.DXA },
        shading: { fill: "E8EFF5", type: ShadingType.CLEAR },
        verticalAlign: VerticalAlign.CENTER,
        children: [new Paragraph({ children: [new TextRun({ text: h, bold: true, size: 19 })] })]
      })
    )
  });
  const body = rows.map((r) =>
    new TableRow({
      children: r.map((c, i) =>
        new TableCell({
          borders: cellBorders,
          width: { size: widths[i], type: WidthType.DXA },
          children: [new Paragraph({ spacing: { after: 40 }, children: [new TextRun({ text: String(c), size: 19 })] })]
        })
      )
    })
  );
  return new Table({ columnWidths: widths, margins: { top: 80, bottom: 80, left: 120, right: 120 }, rows: [head, ...body] });
}

function placeholder(text) {
  return new Table({
    columnWidths: [9360],
    rows: [
      new TableRow({
        children: [
          new TableCell({
            borders: cellBorders,
            shading: { fill: "F4F6F8", type: ShadingType.CLEAR },
            children: [
              new Paragraph({
                alignment: AlignmentType.CENTER,
                spacing: { before: 500, after: 80 },
                children: [new TextRun({ text: "Screenshot placeholder", bold: true, color: "6B7280", size: 22 })]
              }),
              new Paragraph({
                alignment: AlignmentType.CENTER,
                spacing: { after: 500 },
                children: [new TextRun({ text, color: "6B7280", size: 19 })]
              })
            ]
          })
        ]
      })
    ]
  });
}

const pageBreak = () => new Paragraph({ children: [new PageBreak()] });

// ---------- document ----------
const doc = new Document({
  styles: {
    default: { document: { run: { font: "Calibri", size: 22 } } },
    paragraphStyles: [
      { id: "Title", name: "Title", basedOn: "Normal", run: { size: 60, bold: true, color: "0B1E33", font: "Calibri" }, paragraph: { spacing: { after: 120 }, alignment: AlignmentType.CENTER } },
      { id: "Heading1", name: "Heading 1", basedOn: "Normal", next: "Normal", quickFormat: true, run: { size: 30, bold: true, color: "12324F", font: "Calibri" }, paragraph: { spacing: { before: 300, after: 140 }, outlineLevel: 0 } },
      { id: "Heading2", name: "Heading 2", basedOn: "Normal", next: "Normal", quickFormat: true, run: { size: 25, bold: true, color: "1F4E79", font: "Calibri" }, paragraph: { spacing: { before: 220, after: 100 }, outlineLevel: 1 } }
    ]
  },
  numbering: {
    config: [
      { reference: "bullets", levels: [{ level: 0, format: LevelFormat.BULLET, text: "•", alignment: AlignmentType.LEFT, style: { paragraph: { indent: { left: 420, hanging: 240 } } } }] }
    ]
  },
  sections: [
    {
      properties: { titlePage: true, page: { margin: { top: 1200, right: 1200, bottom: 1100, left: 1200 } } },
      headers: {
        default: new Header({
          children: [new Paragraph({ alignment: AlignmentType.RIGHT, children: [new TextRun({ text: "FindLink Lost and Found — Project Report", size: 16, color: "8A97A5" })] })]
        }),
        first: new Header({ children: [new Paragraph({ children: [] })] })
      },
      footers: {
        default: new Footer({
          children: [new Paragraph({ alignment: AlignmentType.CENTER, children: [new TextRun({ text: "Page ", size: 16 }), new TextRun({ children: [PageNumber.CURRENT], size: 16 }), new TextRun({ text: " of ", size: 16 }), new TextRun({ children: [PageNumber.TOTAL_PAGES], size: 16 })] })]
        }),
        first: new Footer({ children: [new Paragraph({ children: [] })] })
      },
      children: [
        // ---------------- TITLE PAGE ----------------
        coverLine("A REPORT", { size: 24, spacing: { before: 400, after: 60 } }),
        coverLine("ON", { size: 24, spacing: { after: 100 } }),
        coverLine("FindLink: Lost and Found", { size: 30, bold: true, spacing: { after: 60 } }),
        coverLine("An Android Application for Reporting and Recovering Lost Items", { size: 22, italics: true, spacing: { after: 260 } }),

        coverLine("Submitted by", { size: 22, italics: true, spacing: { after: 100 } }),
        coverLine("Soyon Chandra Das", { size: 22, spacing: { after: 40 } }),
        coverLine("Roll No: 2307116", { size: 22, spacing: { after: 140 } }),

        coverLine("In", { size: 22, italics: true, spacing: { after: 100 } }),
        coverLine("Computer Science and Engineering", { size: 22, italics: true, spacing: { after: 40 } }),
        coverLine("Second Year Second Term", { size: 22, italics: true, spacing: { after: 140 } }),

        new Paragraph({
          alignment: AlignmentType.CENTER,
          spacing: { after: 120 },
          children: [new ImageRun({
            type: "png",
            data: fs.readFileSync(path.join(__dirname, "kuet-logo-1.png")),
            transformation: { width: 80, height: 91 },
            altText: { title: "KUET", description: "Khulna University of Engineering & Technology emblem", name: "KUET" }
          })]
        }),

        coverLine("Khulna University of Engineering & Technology", { size: 23, bold: true, spacing: { after: 60 } }),
        coverLine("Course No: CSE-2200", { size: 21, bold: true, spacing: { after: 40 } }),
        coverLine("Course Title: Advanced Programming Laboratory", { size: 21, bold: true, spacing: { after: 240 } }),

        coverLine("Submitted to", { size: 22, italics: true, spacing: { after: 140 } }),
        new Table({
          columnWidths: [4680, 4680],
          margins: { top: 0, bottom: 0, left: 0, right: 0 },
          rows: [new TableRow({
            children: [
              [["Ehsanul Karim", false], ["-Lecturer", false], ["Dept. of Computer Science and Engineering", false], ["Khulna University of Engineering & Technology", false]],
              [["Safin Ahmed", false], ["-Assistant Professor", false], ["Dept. of Computer Science and Engineering", false], ["Khulna University of Engineering & Technology", false]]
            ].map((block) =>
              new TableCell({
                borders: { top: { style: BorderStyle.NONE }, bottom: { style: BorderStyle.NONE }, left: { style: BorderStyle.NONE }, right: { style: BorderStyle.NONE } },
                width: { size: 4680, type: WidthType.DXA },
                children: block.map(([text]) =>
                  new Paragraph({
                    alignment: AlignmentType.CENTER,
                    spacing: { after: 20 },
                    children: [new TextRun({ text, font: "Times New Roman", size: 19, italics: true })]
                  })
                )
              })
            )
          })]
        }),
        pageBreak(),

        // ---------------- TOC ----------------
        // ---------------- 1 ----------------
        h1("1. Overview & Objectives"),
        h2("1.1 Purpose"),
        p("FindLink is an Android application that reconnects people with the things they lose. A student or staff member can report a lost item, a finder can report a found item, and the two sides can talk directly inside the app. When ownership is claimed, the app verifies the claimant with a code that it issues itself, so an item is only handed over to the person who can prove it belongs to them."),
        p("The application is built for a campus environment where hundreds of items — ID cards, wallets, keys, bottles, umbrellas — change hands every semester, and most of them are never returned because there is no shared record of who found what."),
        h2("1.2 Problem statement"),
        bullet("Lost-and-found notices are scattered across notice boards, Facebook groups and messaging apps, so a finder and an owner rarely see each other's posts."),
        bullet("Anyone can claim a found item; there is no way to check whether the claimant is the real owner before handing it over."),
        bullet("When the owner cannot be found at all, the finder has no formal place to deposit the item and no record of the handover."),
        h2("1.3 Objectives"),
        bullet("Provide a single searchable feed of lost and found reports, filterable by type and category."),
        bullet("Enable real-time, two-way messaging between the finder and the owner for every item."),
        bullet("Verify ownership with an app-issued 6-digit security code that is never typed by the user at post time."),
        bullet("Offer a formal deposit route through a lost & found desk, with a receipt code and office details."),
        h2("1.4 Scope and target users"),
        p("The current release targets students and campus staff. It supports email/password accounts, image attachments, chat, ownership verification and office deposits. It runs on Android 7.0 and above and was tested on a Xiaomi 12S running Android 15."),

        // ---------------- 2 ----------------
        h1("2. System Architecture & Technology Stack"),
        p("FindLink follows a three-layer structure. Activities and fragments render the interface, small repository/util classes hold the business rules, and Firebase provides authentication and a real-time document database."),
        dataTable(
          ["Layer", "Responsibility", "Key classes"],
          [
            ["Presentation", "Screens, fragments, adapters, layouts", "MainActivity, ChatActivity, ItemDetailsActivity, HomeFragment, SearchFragment, ChatAdapter"],
            ["Domain / rules", "Ownership verification, chat ids, code generation, uploads", "ClaimVerifier, SecurityCode, ClaimCode, ChatRepository, ImgbbUploader"],
            ["Data", "Authentication, documents, real-time listeners", "FirebaseAuth, Cloud Firestore (users, items, chats, messages)"]
          ],
          [1900, 4260, 3200]
        ),
        new Paragraph({ spacing: { after: 120 }, children: [] }),
        h2("2.1 Technology stack"),
        dataTable(
          ["Component", "Technology"],
          [
            ["Language / UI", "Java, AndroidX, Material Components 3"],
            ["Minimum / target SDK", "24 / 35 (edge-to-edge on Android 15)"],
            ["Authentication", "Firebase Authentication — email & password"],
            ["Database", "Cloud Firestore with snapshot listeners and transactions"],
            ["Images", "imgbb REST API (upload) + Glide (display)"],
            ["Camera", "ActivityResultContracts.TakePicture with FileProvider"],
            ["Gallery", "ActivityResultContracts.GetContent"],
            ["Build", "Gradle 8, google-services plugin, Firebase BOM"]
          ],
          [2800, 6560]
        ),
        h2("2.2 Navigation model"),
        p("The app opens on a splash screen, then a four-tab home (Home, Search, Post, Profile) hosted by MainActivity. Fragments are added once and shown/hidden by tag so list position and scroll state survive tab switches. Chat, item details, claim, office deposit and inbox are separate activities launched with explicit intents."),
        pageBreak(),

        // ---------------- 3 ----------------
        h1("3. Feature Walkthrough"),
        p("Each feature below is shown with a screenshot captured from the running application on the test device."),

        h2("3.1 Authentication"),
        p("Users register with a name, email and password. Firebase Authentication creates the account, and a matching document is written to the users collection so the name and email can be shown everywhere in the app. On every launch the splash screen checks the session, so a logged-in user resumes directly at the home screen."),

        h2("3.2 Home dashboard"),
        p("The home screen is a live dashboard rather than a static menu. It greets the signed-in user, shows unread messages, offers a search shortcut, displays live counts of lost, found and deposited items, provides four quick actions and category filters, and lists the reports waiting at lost & found desks."),
        ...figure(SHOT("home/02_home_final.png"), "Figure 3.1 — Home dashboard with live statistics and quick actions"),

        h2("3.3 Posting a report"),
        p("A single form handles both report types. The user chooses Lost or Found, enters the item name, category, location, contact number and a description that doubles as a verification question, and may attach a photo. Tapping the photo card opens a Material dialog with two choices — Camera or Gallery — so the user is never thrown straight into the gallery."),
        twoFigures(SHOT("verify_code/10_add_post.png"), "Figure 3.2 — Post form", SHOT("verify/07_photo_dialog.png"), "Figure 3.3 — Camera / gallery chooser"),
        p("For a Lost report the app immediately generates a six-digit code and shows it once in a dialog; the same code is always available later on the item's details screen. Nothing about the code is typed by the user, which removes the weakest link in a manual scheme."),

        h2("3.4 Search with type filters"),
        p("Search matches the title, category and location of every report. Four filter chips — All, Lost, Found and At office — let a user browse a whole set without typing anything, and typing narrows the same filtered set. Results are sorted newest first and the counter states exactly what is being shown."),

        h2("3.5 Item details"),
        p("The details screen shows the photo, category chip, location, contact row (tap to dial), description and the report type. The action button changes with context: a visitor can message the finder or claim a found item, while the owner sees their security code and office options."),

        h2("3.6 Real-time chat and inbox"),
        p("Every item owns one conversation per pair of users, identified by a deterministic chat id. Messages are written to a sub-collection and both sides listen with a snapshot listener, so a reply appears without refreshing. Sent and received bubbles are styled differently, the last message is marked Seen when the other side reads it, and the inbox lists threads with unread dots. Opening a thread from the inbox passes the exact chat id, which guarantees both participants see the same conversation."),
        twoFigures(SHOT("verify_code/02_chat_verified.png"), "Figure 3.4 — Conversation with verified-owner badge", SHOT("chat/17_inbox_chat.png"), "Figure 3.5 — Inbox thread list"),

        h2("3.7 Ownership verification"),
        p("Ownership is proved with a code that the app issues, never one the user invents. When someone finds an item and opens the owner's lost report, the chat shows an amber banner: the owner is not verified yet. The finder asks for the code, enters it, and the app recomputes a salted SHA-256 hash per lost report and compares it with the stored hash. A match turns the banner green and adds a VERIFIED OWNER chip for both participants; a mismatch leaves the claim unverified, so the finder knows not to hand the item over."),
        p("The same rule is applied on the claim screen of a found report: the claimant types the code the finder gave them, and the claim message is marked verified only when the hash matches."),

        h2("3.8 Lost & found office deposit"),
        p("A finder who cannot trace the owner can deposit the item at one of four desks — Campus Security, Student Affairs, the Police Lost & Found desk or the City Lost Property Office. The app stores the chosen office, its address and opening hours, the deposit time and a receipt code such as FL-4821KM, then shows a receipt dialog. From that point the item leaves the active feed and appears under “Waiting at lost & found desks”, where the owner can find it and bring the code and an ID."),
        ...figure(SHOT("security/03_office_deposit.png"), "Figure 3.6 — Choosing a lost & found desk"),
        pageBreak(),

        // ---------------- 4 ----------------
        h1("4. External API Integration"),
        h2("4.1 imgbb image upload API"),
        p("Photos are uploaded to imgbb through its REST API and only the returned URL is stored in Firestore. The app resizes the bitmap to a maximum dimension of 1080 pixels, compresses it to JPEG at 70% quality, encodes it as Base64 and sends a multipart/form-data POST with HttpURLConnection."),
        dataTable(
          ["Property", "Value"],
          [
            ["Endpoint", "POST https://api.imgbb.com/1/upload"],
            ["Authentication", "API key passed as a query parameter"],
            ["Request body", "multipart/form-data — field name image (Base64 string)"],
            ["Response fields used", "data.url (full image), data.thumb.url, success, status"],
            ["Timeouts", "15 s connect, 30 s read; retry surfaced to the user as a toast"]
          ],
          [2600, 6760]
        ),
        new Paragraph({ spacing: { after: 160 }, children: [] }),
        p("The screenshot below is the real response captured from the API during development. The upload succeeded (success: true, status: 200) and returned a public URL that the app stores and Glide renders."),
        new Paragraph({
          alignment: AlignmentType.CENTER,
          spacing: { before: 120, after: 40 },
          children: [new ImageRun({ type: "png", data: fs.readFileSync(path.join(__dirname, "imgbb-api-response.png")), transformation: { width: 600, height: Math.round((600 * 35) / 100) }, altText: { title: "imgbb response", description: "JSON response from imgbb upload API", name: "imgbb-api-response" } })]
        }),
        new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 200 }, children: [new TextRun({ text: "Figure 4.1 — Live JSON response from the imgbb upload API", italics: true, size: 18, color: "555555" })] }),
        h2("4.2 Why an external image host"),
        p("Firebase Storage now requires a billing-enabled (Blaze) project for new deployments. To keep the project free while still storing photos, imgbb was chosen: it offers a simple HTTP API, no SDK, and a CDN URL that Glide can load directly. The trade-off is that images are public and depend on a third-party quota; the abstraction is isolated in ImgbbUploader, so switching to Firebase Storage later means changing one class."),

        // ---------------- 5 ----------------
        h1("5. Database & Firebase Configuration"),
        h2("5.1 Firebase Authentication"),
        p("Email/password authentication is enabled in the Firebase console. Registration calls createUserWithEmailAndPassword; login calls signInWithEmailAndPassword; logout clears the session and the app returns to the login screen. A guard in MainActivity also redirects to login if no session exists, so a signed-out user can never see the home screen."),
        dataTable(
          ["Firebase setting", "Value used in this project"],
          [
            ["Authentication provider", "Email / password (enabled in Firebase Console → Authentication → Sign-in method)"],
            ["Account records", "Created by the app; profile name and email mirrored to users/{uid}"],
            ["Database", "Cloud Firestore, native mode, single region"],
            ["Realtime Database", "Not enabled — Firestore chosen for document queries and array-contains"],
            ["Cloud Storage", "Not enabled — images hosted on imgbb (see section 4.2)"],
            ["Security rules", "Authenticated access during development; production rules described in 5.4"]
          ],
          [2800, 6560]
        ),
        new Paragraph({ spacing: { after: 120 }, children: [] }),
        h2("5.2 Cloud Firestore data model"),
        dataTable(
          ["Collection", "Document id", "Main fields", "Used by"],
          [
            ["users", "uid", "name, email, userId, createdAt", "Profile, chat names"],
            ["items", "auto id", "itemId, userId, title, category, location, description, reportType, status, phone, timestamp, imageUrl, securityCode, securityHash, claimCode, office*", "Home, Search, Details, Claim, Office"],
            ["chats", "itemId__uidA__uidB", "chatId, itemId, itemTitle, postOwnerId, participants[], participantNames{}, lastMessage, lastMessageAt, lastRead{}, verifiedOwnerId", "Inbox, Chat"],
            ["chats/{id}/messages", "auto id", "senderId, senderName, text, createdAt, type, verified", "Chat thread"]
          ],
          [1700, 2100, 3860, 1700]
        ),
        h2("5.3 Queries and real-time listeners"),
        bullet("items where status = ACTIVE, and where status = AT_OFFICE, for the dashboard counters and feeds."),
        bullet("items where userId = current uid, for “My posts” and for security-code verification."),
        bullet("chats where participants array-contains current uid, ordered in memory by lastMessageAt."),
        bullet("chats/{id}/messages ordered by createdAt, consumed through addSnapshotListener so both users update instantly."),
        bullet("A Firestore transaction creates the chat document only once, and a WriteBatch writes a message and the chat summary atomically."),
        h2("5.4 Security rules"),
        p("During development the rules are open to authenticated clients so the demo works without a backend; the intended production rule set restricts every collection to signed-in users and limits writes to the document owner."),
        new Paragraph({
          spacing: { after: 160 },
          shading: { fill: "F4F6F8", type: ShadingType.CLEAR },
          children: [new TextRun({ text: "match /items/{id} { allow read: if request.auth != null; allow write: if request.auth.uid == resource.data.userId; }", font: "Consolas", size: 18 })]
        }),
        h2("5.5 Realtime Database and file storage"),
        p("Cloud Firestore was selected over the Realtime Database because the data is document-shaped and needs compound queries plus array-contains. The Realtime Database is therefore not enabled for this release; if a live “online now” indicator is added later, it will use RTDB because presence is a small, fast-changing tree. Firebase Storage is not used because images are hosted on imgbb, as explained in section 4.2."),
        pageBreak(),

        // ---------------- 6 ----------------
        h1("6. Testing & Verification"),
        p("The application was built and installed on a Xiaomi 12S (Android 15) with every screen checked visually. Business rules were verified with instrumented tests that run against the real Firebase project, so they exercise the same documents the app uses."),
        dataTable(
          ["Test", "What it proves", "Result"],
          [
            ["SecurityCodeTest", "4–6 digit validation, salted hashing per item, correct code true / wrong code false, verified claim persisted", "Passed"],
            ["ClaimVerifierTest", "Handover code match, security-code match, and no-match handling", "Passed"],
            ["OwnerVerifyTest", "In-chat verification: correct code true, wrong code false, other user's code false, badge saved", "Passed"],
            ["ChatImprovementTest", "Atomic message + summary, chat re-open keeps last message, unread clears after read, verified flag stored", "Passed"],
            ["OfficeDepositTest", "Deposit code format FL-0000AA, near-unique generation, item flips to AT_OFFICE with office fields", "Passed"],
            ["WipeAllDataTest", "Data reset routine deletes users, items, chats (with messages) and confirms empty collections", "Passed"]
          ],
          [2400, 5760, 1200]
        ),
        h2("6.1 Manual checks"),
        bullet("Keyboard opening on the post form no longer produces a blank screen after the IME inset fix."),
        bullet("Headers clear the status bar on Android 15 edge-to-edge devices."),
        bullet("Camera capture works through FileProvider and the photo appears in the post."),
        bullet("A found-item chat shows no verification banner, by design; a lost-item chat does."),

        // ---------------- 7 ----------------
        h1("7. Challenges & Solutions"),
        h2("7.1 Unreadable text across the app"),
        p("The first build mixed a DayNight theme with hard-coded colours, so dark text landed on dark surfaces. One light palette is now forced for values and values-night, with colours centralised in colors.xml."),
        h2("7.2 Android 15 edge-to-edge headers"),
        p("Targeting SDK 35 makes edge-to-edge mandatory, so headers slid under the status bar. WindowInsetsUtil now applies the top inset per header; an earlier attempt that also padded for the keyboard caused a white screen from a double IME inset."),
        h2("7.3 Messages that never arrived"),
        p("The message and the chat summary were written in two separate operations, so a failed summary update left the inbox stale, and two users opening a chat at the same time could overwrite each other's summary. They are now written in one WriteBatch, and chat creation runs inside a transaction."),
        h2("7.4 “FindLink user” everywhere"),
        p("Accounts created before the profile document existed had no name to show. A UserProfileSync helper now guarantees a users document with a name and email, deriving a readable name from the email prefix when the account never stored one."),
        h2("7.5 Verifying an owner who never posted"),
        p("A claimant with no lost report has no code to prove anything. The code is now issued by the app: every lost report receives one at creation, and the finder checks it inside the chat. Claimants without a report are told to ask the finder, and cannot become verified without a matching code."),

        // ---------------- 8 ----------------
        h1("8. Limitations & Future Work"),
        h2("8.1 Current limitations"),
        bullet("No push notifications: a message is visible only when the app is opened."),
        bullet("Security codes are verified on the client; a production build should move the comparison to Cloud Functions or tighten the rules."),
        bullet("The lost & found desk list is fixed in code, and there is no office-side dashboard to mark an item as collected."),
        bullet("Message ordering uses the device clock, so two phones with very different times could show messages out of order."),
        h2("8.2 Planned improvements"),
        bullet("Firebase Cloud Messaging for message and claim notifications."),
        bullet("An office account role that can receive deposits and confirm collection."),
        bullet("Server timestamps (FieldValue.serverTimestamp) for ordering, plus pagination for long threads."),
        bullet("An image moderation step and a size/quota guard before upload."),
        pageBreak(),

        // ---------------- 9 ----------------
        h1("9. Conclusion"),
        p("FindLink turns a scattered, trust-based process into a recorded one. Lost and found reports live in a single searchable feed, two people can talk in real time about a specific item, and ownership is proved with a code the app itself issues — not one the claimant invents. When nobody can be found, the item is deposited at a desk with a receipt code and the owner can locate it from the same app."),
        p("The project also became a practical tour of Android fundamentals: activity lifecycle, explicit and implicit intents, click handling with listeners, multiple layout types, RecyclerView with custom adapters, FileProvider for camera output, HTTP communication with an external API, and a Firebase backend with authentication, document modelling, real-time listeners, transactions and batched writes."),

        h1("References"),
        bullet("Android Developers — Project structure and app modules. https://developer.android.com/studio/projects"),
        bullet("Android Developers — The activity lifecycle. https://developer.android.com/guide/components/activities/activity-lifecycle"),
        bullet("Android Developers — Intents and intent filters. https://developer.android.com/guide/components/intents-filters"),
        bullet("Android Developers — Layouts and ViewGroups. https://developer.android.com/develop/ui/views/layout/declaring-layout"),
        bullet("Android Developers — Create dynamic lists with RecyclerView. https://developer.android.com/develop/ui/views/layout/recyclerview"),
        bullet("Android Developers — Request runtime permissions. https://developer.android.com/training/permissions/requesting"),
        bullet("Android Developers — Connect to the network with HttpURLConnection. https://developer.android.com/develop/connectivity/network-ops/connecting"),
        bullet("Firebase — Authenticate with Firebase using email and password. https://firebase.google.com/docs/auth/android/password-auth"),
        bullet("Firebase — Cloud Firestore data model. https://firebase.google.com/docs/firestore/data-model"),
        bullet("Firebase — Get realtime updates with Cloud Firestore. https://firebase.google.com/docs/firestore/query-data/listen"),
        bullet("Firebase — Transactions and batched writes. https://firebase.google.com/docs/firestore/manage-data/transactions"),
        bullet("imgbb — API v1 documentation. https://api.imgbb.com/"),
        bullet("GeeksforGeeks — Android project folder structure. https://www.geeksforgeeks.org/android/android-project-folder-structure/"),
        bullet("GeeksforGeeks — Activity lifecycle in Android with demo app. https://www.geeksforgeeks.org/android/activity-lifecycle-in-android-with-demo-app/"),
        bullet("GeeksforGeeks — What is Intent in Android. https://www.geeksforgeeks.org/android/what-is-intent-in-android/"),
        bullet("GeeksforGeeks — Handling click events in Android. https://www.geeksforgeeks.org/android/handling-click-events-button-android-java/"),
        bullet("GeeksforGeeks — Firebase Realtime Database vs Firestore. https://www.geeksforgeeks.org/firebase/realtime-database-vs-firestore"),
        bullet("Material Design 3 — Components and theming. https://m3.material.io/"),
        ]
    }
  ]
});

Packer.toBuffer(doc).then((buffer) => {
  fs.writeFileSync(OUT, buffer);
  console.log("written", OUT, buffer.length);
});









