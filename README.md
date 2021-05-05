**Sabacc**

Simple android implementation of Sabacc from Star Wars, using the libGDX framework in android studio.

Rules taken and implemented from http://sabacc.sourceforge.net/rules

Very early alpha, not really in a playable state.

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

At any point after a drawing or betting round, a Sabacc shift may happen. This random event redistributes the values of all cards in each players hand. The only way to prevent this is to place a card into the Interference Field at any time during their turn. The tradeoff of cards in the field being unable to be shifted is that the value is known to all other players.

**Known Bugs**

As of now, the Sudden Demise has not been implemented and the win goes to the firstmost player in order (which will always be the player).

Sabacc Shifts have also not been implemented.

As of now, players hands are capped at 5 cards.

The AI sucks, there is a simple exploit where one can bet more than each AI is willing to pay to force them to fold every round and slowly drain them of credits through the ante.

The only game breaking bug I have encountered as of now is that when the game falls to a low number of players (due to players dropping after being unable to afford the ante) and the human player folds, the game gets stuck waiting for the AI to take their turn. Not really sure why this is happening but makes the game unplayable to a conclusion.