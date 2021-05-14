**Sabacc**

Simple android implementation of Sabacc from Star Wars, using the libGDX framework in android studio.

Rules taken and implemented from http://sabacc.sourceforge.net/rules

Very early alpha, not really in a playable state. To load to build the APK and test on your device, open the build.gradle file in Android Studio.

**Deck Configuration**

The Sabacc deck is composed of 76 cards.
There are four suits, in Sabers, Flasks, Coins, and Staves.
 - Values 1 - 11
 - Ranked Cards:
    - Commander (12)
    - Mistress (13)
    - Master (14)
    - Ace (15)
Additionally, there are two copies each of eight face cards of non-positive values.
 - The Star (-17)
 - The Evil One (-15)
 - Moderation (-14)
 - Demise (-13)
 - Balance (-11)
 - Endurance (-8)
 - Queen of Air and Darkness (-2)
 - Idiot (0)

**Goal of the Game**

The goal of Sabacc is to win credits through various hands. There are two pots: the main pot and the Sabacc pot. Each hand starts with each player placing an ante into both pots, and subsequent bets will be placed only into the main pot.

There are two types of winning hands in Sabacc:
 - A pure Sabacc, a hand with value exactly 23 or -23. There is a special hand called the Idiot's Array that is composed of The Idiot (0), and a 2 and 3 of any suit (a literal 023). The Idiot's Array is the highest hand in the game. A player who holds a pure Sabacc at the end of a round wins both the Main Pot and the Sabacc Pot.
 - If nobody has a pure Sabacc, the winner is the player with the next greatest value up to 23 (or -23). A player with a value greater than 23, less than -23, or exactly 0 has Bombed Out and must pay an amount equal to the Main Pot into the Sabacc Pot.

Play proceeds until players can no longer afford to pay the ante, in which case they drop from the game.

**How to Play**

After the ante, there is a preliminary betting round. Each player may either check, raise, or fold. Each player must match the previous bet in order to stay in the hand.

After the preliminary betting round, each player is dealt two cards. Then each player can either draw a card, stand, or call the hand. The hand cannot be called in the first 4 rounds of the game (the pot building phase).

Play then alternates between Drawing Rounds and Betting Rounds, up until the hand is called.

Once the hand is called, there is one last betting round beginning with the player who called the hand. Then hands are revealed, any player who Bombs Out must pay the penalty to the Sabacc Pot. And then hand values are compared.

**Sudden Demise**

In the event that multiple players have the same winning hand, a Sudden Demise is enacted between them. Each player in the Sudden Demise is dealt an additional card and hand values are compared again. If all players involved in a sudden demise bomb out, they do not have to pay into the Sabacc Pot but none are eligible to win the Main Pot. The Main Pot then goes to the player with the next best hand who has not Bombed Out.

**Sabacc Shift**

At any point after a player draws or bets, a Sabacc shift may happen. This random event redistributes the values of all cards in each players hand. The only way to prevent this is to place a card into the Interference Field at any time during their turn. The tradeoff of cards in the field being unable to be shifted is that the value is known to all other players.

**Known Bugs**

 - The font I am currently using (AnakinMono) does not have a negative sign, so your hand value might look a little strange. Working on fixing that as it is annoying, but does not affect gameplay.

 - Things get a little weird sometimes once the player drops from the game. There may be the occasional bug where the game just sits there or crashes when all players have folded and at least one player has already dropped. Not sure if it was fixed or not by another update

 - As of now, Sudden Demise only happens exactly once per "hand value". So if a Sudden Demise happens and there are multiple players with the same score, the first one will get the win. This seems exceedingly unlikely in practice.

 - If the screen is resized, things get weird

**Current Roadmap**

 - Implement the Sabacc Shift and the Interference Field. A player can place cards into the field by double clicking a card (to lock it) and a small icon will be displayed on it. Opponent's interference cards have a blue border and are displayed underneath their hand (so clicking their tab will have multiple rows of cards).

 - Large AI update once everything is implemented to tie it all together and make the game actually playable. They dont actually use the interference field right now, but the code and UI works for them

**Stuff that can be done at any time**
 (In no particular order)

 - Different message colours along with minor updates to buttons and stuff

    - Allow messages to be longer than the screen and wrap around to new lines. Also allow the player to scroll through previous messages efficiently

 - A main menu that leads to new game, settings, and rules screens

 - Add a delay between dealing cards when enacting Sudden Demise

 - Player profiles, to save and load your profile and have different games. Can add new AI's that play differently and have higher stakes tables with steeper buy in prices and antes.

 - Clicking an opponents card tells you what card it is, probably to be done in the UI update. This might be moderately difficult.

 - Clean up the code for both Betting and Drawing, probably using State Machines