#include <stdio.h>
#include <stdlib.h>
#include <curses.h>
#include <ctype.h>
#include <sys/select.h>
#include <time.h>
#include <string.h>

#define INIT_LENGTH 7
#define MAX_NUMBER_OF_OBSTACLES 13
#define MIN_NUMBER_OF_OBSTACLES 8
#define SNAKE_CHAR '*'
#define FOOD_CHAR '@'
#define OBSTACLE '+'
#define S_TO_WAIT 0
#define MILIS_TO_WAIT 100000
#define INIT_X 12
#define INIT_Y 10
#define FOREVER 1
#define KEYBOARD 0
#define GAME_OVER -1
#define EXIT -1
#define TRUE 1
#define FALSE 0
#define MAX_NAME 101
#define MAX_PLAYERS 30

/* Structura point ce contine coordonatele unui punct pe ecran */
typedef struct point {
	int x, y;
} point;

/* Structura sarpelui, lungimea si coordonatele fiecarui caracter de pe ecran din sarpe */
typedef struct snakeparts {
	int capacity;
	int length;
	point *part; /* *part va contine coordonatele pentru fiecare punct al sarpelui */
} snakeparts;

typedef struct direction {
	int up, down, left, right;
} direction;

typedef struct player {
	int score;
	char name[MAX_NAME];
} player;

int moveSnake(snakeparts snake, point new_head); /* Muta sarpele, sterge coada si adauga un nou caracter pentru cap */
int readNewMove(direction *autoMove, point head, point neck); /* Citeste noua directie de mutare a sarpelui */
point nextMove(point head, direction autoMove); /* Stabileste noile coordonate pentru capul sarpelui */
point generate_food(snakeparts snake, point *obstacle, int nrObstacles, int ymax, int xmax, int ymin, int xmin);
void BorderScreen(int *ymax, int *xmax, int *ymin, int *xmin); /* Creeaza chenarul pentru joc */
int checkInBroders(point new_head, int ymax, int xmax, int ymin, int xmin); /* Verifica daca sarpele se afla in chenar */
int generate_obstacles(snakeparts snake, point *obstacle, int ymax, int xmax, int ymin, int xmin);
int hitObstacle(point new_head, point *obstacle, int nrObstacles); /* Verifica daca sarpele s-a lovit de obstacol */
void addPlayer(player newPlayer, player *players, int *nrPlayers); /* Adauga un player in vectorul players */

int main() {

	int ymax, xmax; /* Numarul maxim de coloane si de linii */
	int ymin, xmin;
	int i, nfds, activity, CheckIfOK, score = 0, nrObstacles;
	int ModObstacles, choice;
	long miliseconds = MILIS_TO_WAIT;
	struct timeval timeout;
	WINDOW *wnd = initscr();
	snakeparts snake;
	point new_head, food, obstacle[MAX_NUMBER_OF_OBSTACLES];
	direction autoMove;
	fd_set read_descriptors;
	FILE *f;
	player players[MAX_PLAYERS], newPlayer;
	int nrPlayers = 0;
	char fileNameHighscores[20];

	clear();
	srand( time(NULL) );

	/* Stabilesc numarul maxim de linii si coloane ymax - numarul de linii, xmax - numarul de coloane*/
	getmaxyx(wnd,ymax, xmax);

	/* Verific daca jocul se poate juca cu numarul maxim de linii si de coloane obtinut anterior */
	if( (xmax <= 13) || (ymax <= 13) ) {
		mvaddstr(0, 0, "Fereastra prea mica pentru a putea juca!");
		mvaddstr(1, 0, "Apasa orice tasta pentru a opri jocul.");
		refresh();
		getch();
		endwin();
		return 0;
	}

	box(wnd, '|', '-');
	mvaddstr(1, xmax/2 - 3, "SNAKE!");
	mvaddstr(3, 2, "1. Mod cu obstacole.");
	mvaddstr(4, 2, "2. Mod fara obstacole.");
	mvaddstr(5, 2, "3. EXIT");
	mvaddstr(ymax - 2, 2, "Apasati tasta corespunzatoare alegerii dorite: ");
	mvaddstr(10, 2, "Instructiuni:");
	mvaddstr(11, 2, "FOOD_CHAR - mancare, consumi mancare => sarpele creste.");
	mvaddstr(12, 2, "OBSTACLE - obstacole, te lovesti de ele => GAME OVER!");
	mvaddstr(13, 2, "Viteza creste la 2 patratele de lungime castigate.");

	/* Citesc de la tastatura una dintre cele trei valori 1, 2, 3 */
	do {

		/* In cazul in care a fost introdusa o optiune invalida, o sterg. */
		mvaddstr(ymax - 2, 49, "                   ");
		refresh();
		
		mvscanw(ymax - 2, 49, "%d", &choice);
	} while(choice < 1 || choice > 3);

	if(choice == 3 /*EXIT*/) {
		endwin();
		return 0;
	}
	if(choice == 1) {
		ModObstacles = TRUE;
	}
	else {
		ModObstacles = FALSE;
	}

	/* Sterg ecranul */
	clear();

	/* Caracterele sunt citite imediat - fără 'buffering' */
	cbreak();

	/* Nu se mai afiseaza caracterele introduse */
	noecho();

	/* Ascund cursorul */
	curs_set(FALSE);

	/* Pregatesc chenarul pentru joc */
	box(wnd, '|', '-');
	BorderScreen(&ymax, &xmax, &ymin, &xmin);

	/* Adaug sarpele, initial de lungime INIT_LENGTH */
	snake.length = INIT_LENGTH;
	for(i=0; i<snake.length; i++) {
		move(INIT_Y, INIT_X + i);
		addch(SNAKE_CHAR);
	}
	refresh();

	/* Aloc memorie pentru vectorul ce contine coordonatele fiecarui punct al sarpelui */
	snake.part = (point *)malloc(INIT_LENGTH * sizeof(point));
	snake.capacity = INIT_LENGTH;

	/* Stabilesc coordonatele pentru fiecare punct din corpul sarpelui */
	for(i=0; i<snake.length; i++) {
		snake.part[i].y = INIT_Y;
		snake.part[i].x = INIT_X + i;
	}
	
	nfds = 1; /*Numarul de descriptori */
	FD_ZERO(&read_descriptors); /* Curat multimea de lucru pentru functia select */
	FD_SET(KEYBOARD, &read_descriptors); /* Adaug tastatura la multimea de descritori */

	timeout.tv_sec = S_TO_WAIT;
	timeout.tv_usec = MILIS_TO_WAIT;

	/* Implicit se va misca la stanga */
	autoMove.left = TRUE;
	autoMove.right = FALSE;
	autoMove.up = FALSE;
	autoMove.down = FALSE;

	if(ModObstacles == TRUE) {
		nrObstacles = generate_obstacles(snake, obstacle, ymax, xmax, ymin, xmin);
	}

	/* generate_food intoarce coordonatele pentru food */
	food = generate_food(snake, obstacle, nrObstacles, ymax, xmax, ymin, xmin);

	mvaddstr(1, 1, "Scor: ");
	mvaddstr(ymax, 1, "q - exit | w,a,s,d - muta");


	while(FOREVER) {

		/* Verific daca este apasata o tasta */
		activity = select(nfds, &read_descriptors, NULL, NULL, &timeout);
		if(activity > 0) {

			/* Citesc noua directie de mutare */ /* CheckIfOK este doar o variabila de verificare */
			CheckIfOK = readNewMove(&autoMove, snake.part[0], snake.part[1]); //snake.part[1] este gatul

			/* Verifica daca a fost apasata tasta 'q' */
			if(CheckIfOK == EXIT) {
			move(ymax/2, xmax/2 - 11);
			break;
			}
		}

		/* Stabilesc noile coordonate pentru noua pozitie a capului */
		new_head = nextMove(snake.part[0], autoMove);

		/* Verific daca noua pozitie se afla in chenar */
		CheckIfOK = checkInBroders(new_head, ymax, xmax, ymin, xmin);
		if(CheckIfOK == GAME_OVER) {
			move(ymax/2 - 1, xmax/2 - 4);
			addstr("GAME OVER!");
			move(ymax/2, xmax/2 - 4);
			addstr("GAME OVER!");
			move(ymax/2 + 1, xmax/2 - 4);
			addstr("GAME OVER!");
			getch();
			break;
		}

		if(ModObstacles == TRUE) {

			/* Verific daca sarpele s-a lovit de obstacol */
			CheckIfOK = hitObstacle(new_head, obstacle, nrObstacles);
			if(CheckIfOK == GAME_OVER) {
				move(ymax/2 - 1, xmax/2 - 4);
				addstr("GAME OVER!");
				move(ymax/2, xmax/2 - 4);
				addstr("GAME OVER!");
				move(ymax/2 + 1, xmax/2 - 4);
				addstr("GAME OVER!");
				getch();
				break;
			}
		}

		/* Verific daca capul sarpelui are aceleasi coordonate ca ale mancarii */
		if((new_head.x == food.x) && (new_head.y == food.y)) {

			/* Generez din nou mancare */
			food = generate_food(snake, obstacle, nrObstacles, ymax, xmax, ymin, xmin);
			score++;

			/* Afisez noul scor pe ecran */
			mvprintw(1, 6, "%d", score);

			/* Verific daca mai am memorie pentru adaugarea unui nou punct al sarpelui in vector */
			if(snake.capacity == snake.length) {

				/* Realoc memorie dubland capacitatea */
				snake.capacity *= 2;
				snake.part = (point *)realloc(snake.part, snake.capacity * sizeof(point));
			}

			/* Incrementez lungimea sarpelui, si initializez noul punct ce reprezinta coada cu -1 si -1 */
			snake.length++;
			/* snake.part[snake.length - 1].x = -1;
			snake.part[snake.length - 1].y = -1; */

			/* Deoarece pentru scoruri pare, sarpele a crescut cu doua unitati, maresc viteza */
			if(score % 2 == 0) {

				if(miliseconds > 20000)
					miliseconds -= 10000;
				else if(miliseconds > 8000)
					miliseconds -= 1000;
				else if(miliseconds > 250)
					miliseconds -= 250;
				else miliseconds /= 2;
			}
		}

		/* Mut sarpele, stergand coada si adaugand un nou punct pe ecran ce reprezinta capul sarpelui */
		CheckIfOK = moveSnake(snake, new_head);

		/* moveSnake returneaza GAME_OVER in cazul in care sarpele se loveste de el insusi */
		if(CheckIfOK == GAME_OVER) {
			move(ymax/2 - 1, xmax/2 - 4);
			addstr("GAME OVER!");
			move(ymax/2, xmax/2 - 4);
			addstr("GAME OVER!");
			move(ymax/2 + 1, xmax/2 - 4);
			addstr("GAME OVER!");
			getch();
			break;
		}

		/* Mut coordonatele incepand de la coada spre cap */
		for(i=snake.length - 1; i>=1; i--) {
			snake.part[i] = snake.part[i-1];
		}
		snake.part[0] = new_head;

		refresh();

		/* Reinitializez read_descriptors si timeout */
		FD_SET(KEYBOARD, &read_descriptors);
		timeout.tv_sec = S_TO_WAIT;
		timeout.tv_usec = miliseconds;
	}

	clear(); /* Sterg ecranul */
	echo(); /* Se afiseaza caracterele citite de la tastatura pe ecran */
	box(wnd, '|', '-');
	curs_set(TRUE); /* Afisez cursorul */
	mvaddstr(1, xmax/2 - 8, "Scorul tau este:");
	mvprintw(2, xmax/2, "%d", score);

	/* Citesc numele jucatorului de la tastatura */
	mvaddstr(4, 2, "Numele tau:");
	mvgetstr(5, 2, newPlayer.name);

	newPlayer.score = score;

	/* Initializez toate scorurile cu -1 */
	for(i=0; i<MAX_PLAYERS; i++) {
		players[i].score = -1;
	}

	/* In functie de ModObstacles, aleg fisierul in care scriu scorul jucatorului */
	if(ModObstacles == TRUE) {
		strcpy(fileNameHighscores, "highscObs.bin");
	}
	else {
		strcpy(fileNameHighscores, "highscNoObs.bin");
	}

	/* Verific daca fisierul binar exista */
	f = fopen(fileNameHighscores, "rb+");
	if(f == NULL) {
		/* Daca nu exista in vectorul players adaug doar noul jucator, si creez fisierul */
		f = fopen(fileNameHighscores, "wb+");
		nrPlayers = 1;
		players[0] = newPlayer;
	}
	else {
		/* Daca exista citesc tot din fiser in vectorul players */
		nrPlayers = fread(players, sizeof(players[0]), MAX_PLAYERS, f);

		/* Adaug noul jucator, pastrand ordinea descrescatoare a scorurilor */
		addPlayer(newPlayer, players, &nrPlayers);
	}

	/* Afisez primele 11 scoruri */
	for(i=0; i<nrPlayers && i<=10; i++) {
		mvprintw(8+i, xmax/2 - 6, "%d. %s: %d", i+1, players[i].name, players[i].score);
	}

	fseek(f, 0, SEEK_SET); /* Ma pozitionez la inceputul fisierului */

	/* Scriu vectorul players in fisier */
	fwrite(players, sizeof(players[0]), nrPlayers, f);

	fclose(f); /* Inchid fisierul */
	getch();
	endwin();
	return 0;
}

/* Muta sarpele, sterge coada si adauga un nou caracter pentru cap */
int moveSnake(snakeparts snake, point new_head) {

	point old_tail = snake.part[snake.length - 1];
	int i;

	/* Verific daca sarpele s-a lovit de el insusi */
	for(i=2; i<snake.length; i++) {
		if( (snake.part[i].x == new_head.x) && (snake.part[i].y == new_head.y) ) {
			return GAME_OVER;
		}
	}

	/* Mut capul, adaugand la noile coordonate caracterul SNAKE_CHAR */
	move(new_head.y, new_head.x);
	addch(SNAKE_CHAR);

	/* Sterg coada */
	move(old_tail.y, old_tail.x);
	addch(' ');

	return 0;
}

/* Citeste noua directie de mutare a sarpelui */
int readNewMove(direction *autoMove, point head, point neck) {

	/* autoMove retine directia in care se deplaseaza sarpele automat */

	char userInput;

	/* Va avea toate campurile nule, pentru a reinitializa autoMove inainte de a-l modifica */
	direction initdirection;

	/* Initializez toate campurile cu 0 */
	initdirection.up = FALSE;
	initdirection.down = FALSE;
	initdirection.left = FALSE;
	initdirection.right = FALSE;

	userInput = getchar();
	switch(tolower(userInput)) {
		case 'a': {

			/* Ignor cazul in care sarpele se muta spre dreapta si se cere mutarea brusca spre stanga */
			if(head.x - 1 == neck.x) {
				return 0;
			}

			*autoMove = initdirection;
			autoMove->left = TRUE;
			break;
		}
		case 's': {

			/* Ignor cazul in care sarpele se muta in sus si se cere mutarea brusca in jos */
			if(head.y + 1 == neck.y) {
				return 0;
			}

			*autoMove = initdirection;
			autoMove->down = TRUE;
			break;
		}
		case 'd': {

			/* Ignor cazul in care sarpele se muta spre stanga si se cere mutarea brusca spre dreapta */
			if(head.x + 1 == neck.x) {
				return 0;
			}

			*autoMove = initdirection;
			autoMove->right = TRUE;
			break;
		}
		case 'w': {

			/* Ignor cazul in care sarpele se muta in jos si se cere mutarea brusca in sus */
			if(head.y - 1 == neck.y) {
				return 0;
			}

			*autoMove = initdirection;
			autoMove->up = TRUE;
			break;
		}
		case 'q': return EXIT;
	}

	return 0;
}

/* Stabileste noile coordonate pentru capul sarpelui */
point nextMove(point head, direction autoMove) {

	/* Initializez noile coordonate cu cele vechi, dupa care le modific corespunzator */
	point new_head = head;

	if(autoMove.up == TRUE) {
		new_head.y -= 1;
		return new_head;
	}
	if(autoMove.down == TRUE) {
		new_head.y += 1;
		return new_head;
	}
		if(autoMove.left == TRUE) {
		new_head.x -= 1;
		return new_head;
	}
		if(autoMove.right == TRUE) {
		new_head.x += 1;
		return new_head;
	}

	/* Teoretic aici nu se va ajunge niciodata */
	return new_head;
}

point generate_food(snakeparts snake, point *obstacle, int nrObstacles, int ymax, int xmax, int ymin, int xmin) {

	/* Daca anumite conditii nu sunt indeplinite atunci voi regenera mancarea */
	/* Indeplinirea conditiilor inseamna regen = FALSE, altfel regen = TRUE */
	int i, regen;
	point food; /* food va retine coordonatele mancarii */

	do {

		/* Generez mancare in interiorul chenarului de joc */
		food.x = rand() % (xmax - 3);
		food.y = rand() % (ymax - 3);
		regen = FALSE;

		/* Verific daca mancarea este in afara chenarului */
		if(food.x <= xmin + 3) regen = TRUE;
		if(food.y <= ymin + 3) regen = TRUE;

		for(i=0; i<snake.length; i++) {

			/* Verific daca mancarea a fost generata peste corpul sarpelui */
			if((snake.part[i].x == food.x) && (snake.part[i].y == food.y)) {
				regen = TRUE;
				break;
			}
		}

		for(i=0; i<nrObstacles; i++) {

			/* Verific daca mancarea a fost generata peste obstacole */
			if((obstacle[i].x == food.x) && (obstacle[i].y == food.y)) {
				regen = TRUE;
				break;
			}
		}
	} while(regen == TRUE);

	/* Afisez mancarea pe ecran */
	move(food.y, food.x);
	addch(FOOD_CHAR);

	return food;
}

/* Creeaza chenarul pentru joc */
void BorderScreen(int *ymax, int *xmax, int *ymin, int *xmin) {

	int i;

	for(i=1; i<*xmax-1; i++) {
		move(2, i);
		addch('-');
	}
	for(i=1; i<*xmax - 1; i++) {
		move(*ymax - 3, i);
		addch('-');
	}

	*ymax -= 2;
	*xmax -= 1;
	*ymin = 1;
	*xmin = 0;
	return;
}

/* Verifica daca sarpele se afla in chenar */
int checkInBroders(point new_head, int ymax, int xmax, int ymin, int xmin) {

	if((new_head.x <= xmin) || (new_head.x >= xmax))
		return GAME_OVER;

	if((new_head.y <= ymin + 1) || (new_head.y >= ymax - 1))
		return GAME_OVER;

	return 0;

}

int generate_obstacles(snakeparts snake, point *obstacle, int ymax, int xmax, int ymin, int xmin) {

	/* Daca anumite conditii nu sunt indeplinite atunci voi regenera obstacolele */
	/* Indeplinirea conditiilor inseamna regen = FALSE, altfel regen = TRUE */

	int nrObstacles, i, j;
	int regen;

	/* Generez numarul de obstacole intre MIN_NUMBER_OF_OBSTACLES si MAX_NUMBER_OF_OBSTACLES */
	do {
		nrObstacles = rand() % (MAX_NUMBER_OF_OBSTACLES + 1);
	} while(nrObstacles < MIN_NUMBER_OF_OBSTACLES);

	for(i=0; i<nrObstacles; i++) {

		do {

			/* Generez un obstacol in interiorul in interiorul chenarului */
			obstacle[i].x = rand() % (xmax - 3);
			obstacle[i].y = rand() % (ymax - 3);
			regen = FALSE;

			/* Verific daca obstacolul a fost generat in afara chenarului */
			if(obstacle[i].x <= xmin + 3) regen = TRUE;
			if(obstacle[i].y <= ymin + 3) regen = TRUE;

			for(j=0; j<snake.length; j++) {

				/* Verific daca obstacolul a fost generat peste corpul sarpelui */
				if((snake.part[j].x == obstacle[i].x) && (snake.part[j].y == obstacle[i].y)) {
					regen = TRUE;
					break;
				}
			}

			for(j=0; j<i; j++) {

				/* Verific daca obstacolul a fost generat peste un alt obstacol deja existent */
				if((obstacle[j].x == obstacle[i].x) && (obstacle[j].y == obstacle[i].y)) {
					regen = TRUE;
					break;
				}
			}
		} while(regen == TRUE);
	}

	/* Afisez obstacolele generate anterior */
	for(i=0; i<nrObstacles; i++) {
		mvaddch(obstacle[i].y, obstacle[i].x, OBSTACLE);
	}

	return nrObstacles;
}

/* Verifica daca sarpele s-a lovit de obstacol */
int hitObstacle(point new_head, point *obstacle, int nrObstacles) {

	int i;

	for(i=0; i<nrObstacles; i++) {
		if( (new_head.x == obstacle[i].x) && (new_head.y == obstacle[i].y) )
			return GAME_OVER;
	}

	return 0;
}

/* Adauga un player in vectorul players */
void addPlayer(player newPlayer, player *players, int *nrPlayers) {

	int i, pos = -1; /* Initializez cu -1 pentru cazul in care nu mai am unde sa adaug noul jucator */

	/* Adaug noul jucator pe pozitia pe care gasesc primul juctor cu scor mai mic ca scorul noului jucator */
	for(i=0; i<MAX_PLAYERS; i++) {
		if(newPlayer.score >= players[i].score) {
			pos = i;
			break;
		}
	}

	/* Daca nu s-a gasit nicio pozitie, atunci scorul este prea mic, deci nu-l mai adaug in vector */
	/* Asta functioneaza pentru ca am initializat in main() toate scorurile cu -1 */
	if(pos == -1)
		return;

	/* Deplasez toti jucatorii de la MAX_PLAYERS pana la pos */
	for(i=MAX_PLAYERS - 1; i>pos; i--) {
		players[i] = players[i - 1];
	}

	/* Adaug noul jucator pe pozitia pos */
	players[pos] = newPlayer;
	(*nrPlayers)++;

	return;
}