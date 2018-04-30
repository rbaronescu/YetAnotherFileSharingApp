/* Baronescu Andrei-Robert 322CC */

#include <iostream>
#include <map>
#include <queue>
#include <algorithm>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h>
#include <arpa/inet.h>
#include <sys/stat.h>

#define MAX_BUFFER 1024
#define MAX_MSG 974
#define FOREVER 1
#define TRUE 1
#define FALSE 0
#define CLIENT 1
#define SERVER 0
#define NONE -1
#define STILL_NOT_UPLOADED 1

/* for debugging */
void error(std::string s);

/* determina daca sunt date introduse de
 * la tastatura in stdin */
int kbhit();

/* aici verific tipul comenzii, si intorc
 * rezultatul acesteia */
int executeCommand();

/* adauga infisierul de log
 * _side poate fi:
 * CLIENT: deci printeaza si prompt0ul in aces caz.
 * SERVER: printeaza si mesajul corespunzator erorii.
 * NONE: doar printeaza textul primit
 */
void appendToLog(std::string to_write, short _side);

/* codurile de eroare */
void init_error_codes();

/* din 12345 => 12.345,
 * conform cerintei.
 */
std::string digitGrouping(off_t size);

int cmd_login(char *userName);
int cmd_logout();
int cmd_getuserlist();
int cmd_getfilelist();
int cmd_upload(char *fileName);
int cmd_download();
int cmd_share();
int cmd_unshare();
int cmd_delete();
int cmd_quit();

/* socket pentru comunicarea cu server-ul */
int sockfd, n;

/* sting-ul folosit pentru a
 * trimite date clientului
 */
char buffer[MAX_BUFFER];

/* numele user-ului, logat
 * in acest client */
char *userConnected;

/* prompt: '$' sau '>' */
char prompt;

/* retine daca acest client este logat, sau nu */
int loggedIn = FALSE;

/* retine, daca am primit comanda quit
 * de la tastatura
 */
bool quit_cmd_recived = false;

/* structura folosita pentru
 * a transmite fisiere */
typedef struct Msg {
	char payload[MAX_BUFFER - 50];
	short len;
} Msg;

/* clasa ce contine date
 * despre un fiser, care este in
 * curs de upload */
class UploadFile {
public:

	/* numele acestuia */
	std::string name;

	/* pozitia din fisier,
	 * pana unde a fost transmis
	 * continutul din fisier
	 */
	off_t offset;

	/* descriptorul atasat acestuia */
	int fd;

	UploadFile(std::string _name) {
		name = _name;
		offset = 0;
		fd = 0;
	}
};

/* retine lista de fisiere
 * ce sunt incarcate pe server
 * de acest user
 */
std::map<std::string, UploadFile*> _to_upload;

/* coada de fisiere in curs de upload */
std::queue<std::string> _to_upload_queue;

/* coada de fisiere in curs de download */
std::queue<std::string> _to_download_queue;

/* retine mesajele de eroare atasate unui numar */
std::map<int, std::string> _error_codes;

int main(int argc, char **argv) {

	/* adresa de internet a server-ului */
	struct sockaddr_in serv_addr;

	/* doar pentru afisare */
	bool printPrompt = true;

	/* la inceput nu este nici un
	 * utilizator conectat, iar
	 * prompt-ul este '$'
	 */
	userConnected = strdup("");
	prompt = '$';

	/* functie pentru a initializa _error_codes */
	init_error_codes();

	if (argc != 3) {
		error("numar de parametri incorect!");
	}

	/* prin intermediul acestui socket
	 * voi comunica cu server-ul
	 */
	sockfd = socket(AF_INET, SOCK_STREAM, 0);
	if (sockfd < 0)
		error("la deshiderea socket-ului!");

	/* parametrii pentru conexiune */
	serv_addr.sin_family = AF_INET;
	serv_addr.sin_port = htons(atoi(argv[2]));
	inet_aton(argv[1], &serv_addr.sin_addr);

	/* incerc sa creez conexiunea cu server-ul pe sockfd */
	if (connect(sockfd,(struct sockaddr*) &serv_addr,sizeof(serv_addr)) < 0)
		error("la conectare!");

	while (FOREVER) {

		/* daca nu am niciun fisier in coada pentru a fi incarcat/descarcat
		 * pe sau de pe server, si daca nu s-a dat comanda de quit,
		 * atunci citesc comenzi de la tastatura */
		if (_to_upload_queue.empty() && _to_download_queue.empty() && !quit_cmd_recived) {
			
			/* afisez prompt-ul */
			printf("%s%c ", userConnected, prompt);

			/* pentru urmatoarea comanda de
			 * download sau upload
			 */
			printPrompt = true;
			
			/* citesc in buffer */
			memset(buffer, 0, MAX_BUFFER);
			fgets(buffer, MAX_BUFFER-1, stdin);
			
			/* adaug in fisierul de log */
			appendToLog(buffer, CLIENT);
			
			int result = executeCommand(); /* execut comanda, buffer-ul
											 * e declarat global
											 * deci voi verifica
											 * tipul comenzii in alte functii
											 */
			if (result < 0) {
				/* atunci adaug in log codul de
				 * eroare si mesajul corespunator
				 */
				appendToLog(buffer, SERVER);

				/* si il afisez si pe ecran,
				 * _error_codes[x] este un map
				 * ce contine perechiile formate
				 * din codul de eroare si mesajul
				 * atasat acestuia
				 */
				std::cout << result << " " << _error_codes[result];
			}
		} else {

			/* stabilesc daca trebuie afisat prompt-ul */
			if (!quit_cmd_recived && printPrompt) {
				printf("%s%c ", userConnected, prompt);
				fflush(stdout); /* pentru afisare imediata */
				fflush(stdout); /* pentru afisare imediata */
			}

			/* kbhit - foloseste select, petntru
			 * a determina daca sunt introduse 
			 * comezi de la tastatura.
			 * In caz afirmativ, le voi citii,
			 * altfel, verific, daca trebuie sa
			 * efectuez un transfer de fisiere */
			if (!quit_cmd_recived && kbhit()) {

				/* dupa comanda, trebuie sa afisez prompt-ul */
				printPrompt = true;

				/* citesc in buffer */
				memset(buffer, 0, MAX_BUFFER);
				fgets(buffer, MAX_BUFFER-1, stdin);
				
				/* adaug in fisierul de log */
				appendToLog(buffer, CLIENT);

				int result = executeCommand(); /* execut comanda, buffer-ul
											 * e declarat global
											 * deci voi verifica
											 * tipul comenzii in alte functii
											 */
				if (result < 0) {
					/* atunci adaug in log codul de
				 	 * eroare si mesajul corespunator
				 	 */
					appendToLog(buffer, SERVER);

					/* si il afisez si pe ecran,
				 	 * _error_codes[x] este un map
				 	 * ce contine perechiile formate
				 	 * din codul de eroare si mesajul
				 	 * atasat acestuia
				 	 */
					std::cout << result << " " << _error_codes[result];
				}
			} else {

				/* nu trebuie sa afisez prompt-ul,
				 * nu este introdus nimic de la tastatura
				 */
				printPrompt = false;
				
				/* daca nu am de efectuat niciun transfer de fisiere
				 * si s-a primit de la tastatura comanda quit,
				 * atunci inchid clientul
				 */
				if (_to_upload_queue.empty() && _to_download_queue.empty() && quit_cmd_recived) {
					return cmd_quit();
				}

				/* daca mai am de incarcat fisere pe server */
				if (!_to_upload_queue.empty()) {

					/* il iau pe primul din aceasta coada */
					std::string fileName = _to_upload_queue.front();
					_to_upload_queue.pop();

					/* apelez cmd_uplaod pe acest fiser */
					memset(buffer, 0, MAX_BUFFER);
					sprintf(buffer, "upload %s\n", fileName.c_str());
					if (executeCommand() != STILL_NOT_UPLOADED) {
						/* si daca, inca nu s-a terminat
						 * de incarcat pe server, il pun
						 * din nou in coada
						 */
						_to_upload_queue.push(fileName);
					}
				}

				/* daca mai am de descarcat fisere de pe server */
				if (!_to_download_queue.empty()) {

					/* il iau pe primul din aceasta coada */
					std::string arg = _to_download_queue.front();
					_to_download_queue.pop();

					/* apelez cmd_download pe acest fiser */
					memset(buffer, 0, MAX_BUFFER);
					sprintf(buffer, "download %s\n", arg.c_str());
					
					/* si cmd_download va introduce din nou,
					 * acest fisier in coada, daca va fi nevoie
					 */
					executeCommand();
				}
			}
		}
	}

	return 0;
}

/* adauga infisierul de log
 * _side poate fi:
 * CLIENT: deci printeaza si prompt0ul in aces caz.
 * SERVER: printeaza si mesajul corespunzator erorii.
 * NONE: doar printeaza textul primit
 */
void appendToLog(std::string to_write, short _side) {

	/* numele fisierului */
	char logFileName[100];
    sprintf(logFileName, "client-%d.log", getpid());

    /* deschid fisierul cu append */
	mode_t mode = S_IRUSR | S_IWUSR | S_IRGRP | S_IROTH;
	int fd = open(logFileName, O_CREAT | O_APPEND | O_WRONLY, mode);

	std::string text;
	if (CLIENT == _side) {
		text = userConnected;
		text += prompt;
		text += " ";
		text += to_write;
	} else if (SERVER == _side) {
		text = to_write + " ";
		text += _error_codes[atoi(to_write.c_str())];
	} else {
		text = to_write;
	}

	/* scriu in fisier */
	write(fd, text.c_str(), text.length());
	close(fd);
}

/* aici verific tipul comenzii, si intorc
 * rezultatul acesteia */
int executeCommand() {

	/* iau primul cuvant din buffer */
	char *tmp = strdup(buffer);
	char *token = strtok(tmp, " ");

	if (0 == strcmp(token, "login")) {
		/* apelez cmd_login cu urmatorul cuvant */
		return cmd_login(strtok(NULL, " "));
	}
	if (0 == strcmp(token, "logout\n")) {
		return cmd_logout();
	}
	if (0 == strcmp(token, "getuserlist\n")) {
		return cmd_getuserlist();
	}
	if (0 == strcmp(token, "getfilelist")) {
		return cmd_getfilelist();
	}
	if (0 == strcmp(token, "upload")) {
		return cmd_upload(strtok(NULL, " \n"));
	}
	if (0 == strcmp(token, "download")) {
		return cmd_download();
	}
	if (0 == strcmp(token, "share")) {
		return cmd_share();
	}
	if (0 == strcmp(token, "unshare")) {
		return cmd_unshare();
	}
	if (0 == strcmp(token, "delete")) {
		return cmd_delete();
	}
	if (0 == strcmp(token, "quit\n")) {
		return cmd_quit();
	}

	/* comanda primita nu e recunoscuta,
	 * deci nu o voi mai trimite la server */
	return -10;
}

/* download functioneaza astfel:
 *	retine intr-o coada fiserele ce trebuiesc descarcate
 * 	si trimite intr-una cerere la server, atunci cand nu
 * 	se primeste o comanda de la tastatura, pentru a cere
 * 	parti din acest fisier, iar server-ul trimite cate una,
 * 	Si adauga aceasta parte la sfarsitul fisierului.
 * 	Dupa care reiau algoritmul.
 */ 
int cmd_download() {

	/* verific daca clientul este logat */
	if (!loggedIn) {
		sprintf(buffer, "-1");
		return -1;
	}

	/* parsez comanda primita */
	std::string fileName;
	std::string userName;
	int words = 0; /* numarul de cuvinte din comanda */
	char *tmp = strdup(buffer); /* pentru strtok */
	char *token[3]; /* cuvintele */

	/* separ cuvintele din buffer */
	tmp = strtok(tmp, " \n");
	while (tmp != NULL) {
		token[words++] = strdup(tmp);
		tmp = strtok(NULL, " \n");
	}

	/* daca exista doar 2 cuvinte sau este introdus
	 * caracterul special '@', atunci, user-ul ce detine
	 * fisierul fileName, este user-ul curent
	 */
	if ((2 == words) || (0 == strcmp(token[1], "@"))) {
		memset(buffer, 0, MAX_BUFFER);
		fileName = token[1];
		userName = userConnected;
		sprintf(buffer, "download %s %s\n", userConnected, token[1]);
	} else {
		fileName = token[2];
		userName = token[1];
	}

	/* trimit comanda */
	n = send(sockfd, buffer, strlen(buffer), 0);
    if (n < 0)
    	error("la scriere in socket!");

    /* primesc confirmare */
    memset(buffer, 0, MAX_BUFFER);
    n = read(sockfd, buffer, MAX_BUFFER);
    if (n < 0)
    	error("la citirea din socket!");

    /* daca am primit eroare, o returnez */
    if (0 == strcmp(buffer, "-4")) {
    	return -4;
    } else if (0 == strcmp(buffer, "-5")) {
    	return -5;
    }

    /* trimit confirmare */
    n = send(sockfd, "ACK", 3, 0);
    if (n < 0)
    	error("la scriere in socket!");

    char pid[20]; /* pentru a retine pid-ul */
    sprintf(pid, "%d", getpid());

    /* creez numele fisierului ce
     * urmeaza a fi descarcat */
    std::string tmp_name = pid;
    tmp_name += "_" + fileName;

	/* daca am primit tot fisierul */
    if (0 == strcmp(buffer, "~done~")) {

    	/* primesc rezultatul "0" */
    	memset(buffer, 0, MAX_BUFFER);
    	n = recv(sockfd, buffer, MAX_BUFFER, 0);
    	if (n < 0)
    		error("la citirea din socket!");

    	/* pentru a calcula dimensiunea fisierului */
    	struct stat st;
		stat(tmp_name.c_str(), &st);

		/* creez mesajul care urmeaza a fi printat
		 * si adaugat in log
		 */
    	std::string to_print = userConnected;
    	to_print += prompt;
    	to_print += " Download finished: ";
    	to_print += fileName;
    	to_print += " - ";
    	to_print += digitGrouping(st.st_size);
    	to_print += "\n";

    	std::cout << std::endl << to_print;
    	appendToLog(to_print, NONE);

    	return atoi(buffer);
    }

    /* primesc caracterele si numarul acestora, in
	 * buffer, si le copiez in structura msg de tip Msg
	 */
    Msg msg;
    memset(&msg, 0, sizeof(msg));
    memcpy(&msg, buffer, sizeof(msg));

    /* daca nu l-am terminat de primit,
     * il adaug in coada
     */
    std::string _tmp = userName;
    _tmp += " " + fileName;
    _to_download_queue.push(_tmp);

    /* deschid fisierul si adaug
     * la sfarsit, ceea ce am primit
     */
    mode_t mode = S_IRUSR | S_IWUSR | S_IRGRP | S_IROTH;
    int fd = open(tmp_name.c_str(), O_CREAT | O_APPEND | O_WRONLY, mode);
    write(fd, msg.payload, msg.len);
    close(fd);

    /* primesc rezultatul final de la server "0" */
    memset(buffer, 0, MAX_BUFFER);
    n = recv(sockfd, buffer, MAX_BUFFER, 0);
    if (n < 0)
    	error("la citirea din socket!");

    return atoi(buffer);
}

/* 	upload functioneaza astfel:
 *	Pentru a fi transmis, un fisier este impartit in parti.
 *	Inainte de a transmite urmatoarea parte, verific, daca
 *	pana acum i-am mai transmis ceva din acest fisier.
 *	
 *	Daca DA - atunci ma plasez cu lseek, pe pozitia
 *	din fisier care a fost deja transimsa, si transmit
 *	incepand de acolo.
 *
 *	Daca NU - atunci transmit de la inceput.
 * 	Toate astea, atunci cand nu primesc comanda,
 * 	de la tastatura.
 */ 
int cmd_upload(char *fileName) {
	
	/* verific daca clientul este logat */
	if (!loggedIn) {
		sprintf(buffer, "-1");
		return -1;
	}

	/* trimit comanda */
	n = send(sockfd, buffer, strlen(buffer), 0);
    if (n < 0)
    	error("la scriere in socket!");

    /* primesc confirmare */
    memset(buffer, 0, MAX_BUFFER);
    n = recv(sockfd, buffer, sizeof(buffer), 0);
	if (n <= 0)
		error("in recv / Nu exista conexiune la server!");

	/* daca am primit cod de eroare, il returnez */
	if (0 == strcmp(buffer, "-9")) {
		return -9;
	}

	/* calculez dimensiunea fisierului */
	struct stat st;
	stat(fileName, &st);

	/* daca nu am incarcat pe server, nimic
	 * din acest fisier pana acum, atunci, il
	 * deschid, si incep transmiterea acestuia
	 */
	if (NULL == _to_upload[fileName]) {
		int fd = open(fileName, O_RDONLY);
		if (fd < 0) {
			sprintf(buffer, "-4");
			return -4;
		}

		/* creez instanta UploadFile */
		_to_upload[fileName] = new UploadFile(fileName);
		_to_upload[fileName]->fd = fd;

		/* il adaug in coada */		
		_to_upload_queue.push(fileName);
	}

	/* offset-ul pe care va trebui sa ma plasez inainte
	 * de a-i trimite urmatoarea parte din fisier */
	off_t offset = _to_upload[fileName]->offset;
	
	/* fd pentru acest fisier */
	int fd = _to_upload[fileName]->fd;
	if (fd < 0) {
		sprintf(buffer, "-4");
		return -4;
	}

	/* verific daca i-am trimis deja tot fisierul */
	if(offset >= st.st_size) {

		/* il scot din _to_upload */
		delete _to_upload[fileName];
		_to_upload.erase(fileName);

		/* trimit, mesajul ~done~, deci am terminat
		 * de tranmis fisierul
		 */
		memset(buffer, 0, MAX_BUFFER);
		sprintf(buffer, "~done~");
		n = send(sockfd, buffer, strlen(buffer), 0);
    	if (n < 0)
    		error("la scriere in socket!");

		/* astept raspuns la acest mesaj */
		memset(buffer, 0, MAX_BUFFER);
    	n = recv(sockfd, buffer, sizeof(buffer), 0);
		if (n <= 0)
			error("in recv / Nu exista conexiune la server!");

		/* inchid fisierul */
    	close(fd);

    	/* creez mesajul care urmeaza a fi printat
		 * si adaugat in log
		 */
    	std::string to_print = userConnected;
    	to_print += prompt;
    	to_print += " Upload finished: ";
    	to_print += fileName;
    	to_print += " - ";
    	to_print += digitGrouping(st.st_size);
    	to_print += "\n";

    	std::cout << std::endl << to_print;
    	appendToLog(to_print, NONE);

    	return 1;
	}

	/* daca nu s-a trimis tot fisierul,
	 * trimit de unde am ramas
	 */
	lseek(fd, offset, SEEK_SET);

	/* trimit caracterele si numarul acestora, intr-o
	 * structura, pe care o copiez in buffer */
	Msg msg;
	memset(&msg, 0, sizeof(msg));
	msg.len = read(fd, msg.payload, MAX_MSG);

	/* copiez in buffer */
	memset(buffer, 0, MAX_BUFFER);
	memcpy(buffer, &msg, sizeof(msg));

	/* ii trimit server-ului partea din fisier */
	n = write(sockfd, buffer, MAX_BUFFER);
	if (n < 0)
		error("la scrierea in socket!");

	/* astept confirmare */
	memset(buffer, 0, MAX_BUFFER);
	n = read(sockfd, buffer, sizeof(buffer));
	if (n < 0)
		error("la citirea din socket!");

	/* retin in offset-ul din UploadFile pozitia curenta
	 * din fisier pentru urmatoarea transmisie din acest fisier
	 */
	_to_upload[fileName]->offset = lseek(fd, 0, SEEK_CUR);

	return 0;
}

int cmd_delete() {
	
	/* verific daca clientul este logat */
	if (!loggedIn) {
		sprintf(buffer, "-1");
		return -1;
	}

	/* trimit comanda */
	n = send(sockfd, buffer, strlen(buffer), 0);
    if (n < 0)
    	error("la scriere in socket!");

    /* primesc raspunsul */
    memset(buffer, 0, MAX_BUFFER);
    n = recv(sockfd, buffer, sizeof(buffer), 0);
	if (n <= 0)
		error("in recv / Nu exista conexiune la server!");

	/* daca am primit cod de eroare,
	 * il returnez */
	int result = atoi(buffer);
	if (result < 0) {
		return result;
	} else {
		std::cout << "200 Fisier sters\n";
		appendToLog("200 Fisier sters\n", NONE);
		return result;
	} 
}

/* quit functioneaza astfel:
 * in momentul in care comanda quit
 * este data de la tastatura, apelez 
 * functia quit, ce seteaza
 * quit_cmd_recived = true;
 * dar nu inchide aplicatia deoarece
 * e posibil sa mai fie fisere de transferat
 * in main vrific cand nu mai este niciun 
 * fisier de transferat, si apelez din nou
 * aceasta functie, cu quit_cmd_recived = true
 * de data asta
 */
int cmd_quit() {

	/* verific daca clientul este logat */
	if (!loggedIn) {
		return -1;
	}

	/* daca nu mai exista nicio
	 * conexiune, acest lucru a fost erificat
	 * in main atunci:
	 */
	if (quit_cmd_recived) {

		/* efectuez logout */
		memset(buffer, 0, MAX_BUFFER);
		sprintf(buffer, "logout\n");
		cmd_logout();

		/* trimit comanda quit server-ului */
		memset(buffer, 0, MAX_BUFFER);
		sprintf(buffer, "quit\n");
		n = send(sockfd, buffer, strlen(buffer), 0);
		if (n < 0)
			error("la scriere in buffer");

		/* astept confirmarea */
		n = recv(sockfd, buffer, sizeof(buffer), 0);
		if (n <= 0)
			error("in recv / Nu exista conexiune la server!");

		/* inchid socket-ul */
		close(sockfd);
	} else {
		quit_cmd_recived = true;
	}

	return 0;
}


int cmd_share() {

	/* verific daca clientul este logat */
	if (!loggedIn) {
		sprintf(buffer, "-1");
		return -1;
	}

	/* pregatesc mesajul pentru a fi printat */
	std::string text = "200 Fisierul";
	text += " ";
	text += (buffer + 6);
	text.erase(text.end() - 1); /* sterg '\n' de la sfarsit */
	text += " a fost partajat.\n";

	/* trimit comanda */
	n = send(sockfd, buffer, strlen(buffer), 0);
    if (n < 0)
    	error("la scriere in socket!");

    /* primesc raspunsul */
    memset(buffer, 0, MAX_BUFFER);
    n = recv(sockfd, buffer, sizeof(buffer), 0);
	if (n <= 0)
		error("in recv / Nu exista conexiune la server!");

	/* daca am primit cod de eroare,
	 * il returnez */
	int result = atoi(buffer);
	if (result < 0) {
		return result;
	} else {
		std::cout << text << std::endl;
		appendToLog(text, NONE);
		return result;
	}
}

int cmd_unshare() {

	/* verific daca clientul este logat */
	if (!loggedIn) {
		sprintf(buffer, "-1");
		return -1;
	}

	/* trimit comanda */
	n = send(sockfd, buffer, strlen(buffer), 0);
    if (n < 0)
    	error("la scriere in socket!");

    /* primesc raspunsul */
    memset(buffer, 0, MAX_BUFFER);
    n = recv(sockfd, buffer, sizeof(buffer), 0);
	if (n <= 0)
		error("in recv / Nu exista conexiune la server!");

	/* daca am primit cod de eroare,
	 * il returnez */
	int result = atoi(buffer);
	if (result < 0) {
		return result;
	} else {
		std::cout << "200 Fisierul a fost setat ca PRIVATE\n"; 
		appendToLog("200 Fisierul a fost setat ca PRIVATE\n", NONE);
		return result;
	}
}

/* deoarece in enut nu spune fatpul ca
 * getfilelist trebuie sa repsecte
 * round-robin, sa nu fie blocanta, atunci,
 * in cazul in care lista de fisiere este foarte
 * mare, pur si simplu, o trimit pe bucati pe rand
 * fara a respecta round-robin */
int cmd_getfilelist() {

	/* verific daca clientul este logat */
	if (!loggedIn) {
		sprintf(buffer, "-1");
		return -1;
	}

	/* trimit comanda */
	n = send(sockfd, buffer, strlen(buffer), 0);
    if (n < 0)
    	error("la scriere in socket!");

     while (FOREVER) {

     	/* primesc rezultatul */
		memset(buffer, 0, MAX_BUFFER);
		n = recv(sockfd, buffer, sizeof(buffer), 0);
		if (n <= 0)
			error("in recv / Nu exista conexiune la server!");

		/* daca am primit cod de eroare, il intorc */
		if (0 == strcmp(buffer, "-11")) {
			return -11;
		}

		/* in acest caz ma opresc */
		if (('0' == *buffer) && (1 == strlen(buffer))) {
			break;
		}
		
		/* afisez si adaug in log */
		printf("%s", buffer);
		appendToLog(buffer, NONE);

		/* trimit confirmare */
		n = send(sockfd, "ACK", 3, 0);
		if (n < 0)
			error("in write in socket!");
	}

	return 0;
}

/* deoarece in enut nu spune fatpul ca
 * getuserlist trebuie sa repsecte
 * round-robin, sa nu fie blocanta, atunci,
 * in cazul in care lista de utilizatori este foarte
 * mare, pur si simplu, o trimit pe bucati pe rand
 * fara a respecta round-robin */
int cmd_getuserlist() {
	
	/* verific daca clientul este logat */
	if (!loggedIn) {
		sprintf(buffer, "-1");
		return -1;
	}

	/* trimit comanda */
	n = send(sockfd, buffer, strlen(buffer), 0);
    if (n < 0)
    	error("la scriere in socket!");

    while (FOREVER) {

    	/* primesc raspunsul */
		memset(buffer, 0, MAX_BUFFER);
		n = recv(sockfd, buffer, sizeof(buffer), 0);
		if (n <= 0)
			error("in recv / Nu exista conexiune la server!");

		/* in acest casz ma opresc */
		if (('0' == *buffer) && (1 == strlen(buffer))) {
			break;
		}
		
		/* afisez si adaug in log */
		printf("%s", buffer);
		appendToLog(buffer, NONE);

		/* trimit confirmare */
		n = send(sockfd, "ACK", 3, 0);
		if (n < 0)
			error("in write in socket!");
	}

	return 0;
}

int cmd_login(char *userName) {

	/* trimit comanda */
	n = send(sockfd, buffer, strlen(buffer), 0);
    if (n < 0)
    	error("la scriere in socket!");

    /* primesc raspunsul */
    memset(buffer, 0, MAX_BUFFER);
	n = recv(sockfd, buffer, sizeof(buffer), 0);
	if (n <= 0) {
		error("in recv / Nu exista conexiune la server!");
	} else {
		/* daca primes "0", efectuez login */
		if (0 == atoi(buffer)) {

			/* modific numele user-ului logat */
			free(userConnected);
			userConnected = strdup(userName);
			prompt = '>';
			loggedIn = TRUE;
		}
	}

	return atoi(buffer);
}

int cmd_logout() {

	/* verific daca clientul e logat */
	if (!loggedIn) {
		sprintf(buffer, "-1");
		return -1;
	}

	/* trimit comanda */
	n = send(sockfd, buffer, strlen(buffer), 0);
    if (n < 0)
    	error("la scriere in socket!");

    /* primesc raspunsul */
    memset(buffer, 0, MAX_BUFFER);
	n = recv(sockfd, buffer, sizeof(buffer), 0);
	if (n <= 0)
		error("in recv / Nu exista conexiune la server!");

	/* efectuez logout */
	free(userConnected);
	userConnected = strdup("");
	prompt = '$';
	loggedIn = FALSE;

	return atoi(buffer);
}

/* din 12345 => 12.345,
 * conform cerintei.
 */
std::string digitGrouping(off_t size) {
	
	char _tmp[20];
	sprintf(_tmp, "%jd", size);
	std::string result = _tmp;

	/* in acest caz nu fac nimic */
	if (result.length() <= 3) {
		return result;
	}

	/* intorc string-ul, si adaug din 4 in 4 puncte */
	std::reverse(result.begin(), result.end());

	for (unsigned i = 3; i < result.length(); i += 4) {
		result.insert(i, 1, '.');
	}

	/* intorc string-ul din nou */
	std::reverse(result.begin(), result.end());

	return result;
}

/* determina daca sunt date introduse de
 * la tastatura in stdin */
int kbhit() {

	/* timpul, folosit in select */
    struct timeval tv;
    fd_set fds;

    /* 0.01 secunde */
    tv.tv_sec = 0.01;
    tv.tv_usec = 0;

    /* golsec fds */
    FD_ZERO(&fds);

    /* adaug stdin in fds */
    FD_SET(STDIN_FILENO, &fds);

    /* aplic select */
    select(STDIN_FILENO+1, &fds, NULL, NULL, &tv);
    
    /* returnez daca exista stdin in fds */
    return FD_ISSET(STDIN_FILENO, &fds);
}

/* codurile de eroare */
void init_error_codes() {
	_error_codes[-1] = "Clientul nu e autentificat\n";
	_error_codes[-2] = "Sesiune deja deschisa\n";
	_error_codes[-3] = "User/parola gresita\n";
	_error_codes[-4] = "Fisier inexistent\n";
	_error_codes[-5] = "Descarcare interzisa\n";
	_error_codes[-6] = "Fisier deja partajat\n";
	_error_codes[-7] = "Fisier deja privat\n";
	_error_codes[-8] = "Brute-force detectat\n";
	_error_codes[-9] = "Fisier existent pe server\n";
	_error_codes[-10] = "Fisier in transfer\n";
	_error_codes[-11] = "Utilizator inexistent\n";
}

/* for debugging */
void error(std::string s) { 
	std::cout << "EROARE: " << s << std::endl;
	exit(-1);
}