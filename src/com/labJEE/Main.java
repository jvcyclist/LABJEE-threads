package com.labJEE;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class Main {
	public static void main(String[] args) throws IOException {
		
		/*
		 * Zdefiniowano wczesniej katalog oraz pliki do sortowania.
		 * 
		 * Sa rowniez ograniczenia odnosnie ilosci sortowanych slow

		 */

		
		String fileStorage = "/eclipse-workspace/LabJEE-threads/files";
		long wordLimit = 10;

		final AtomicBoolean teaTime = new AtomicBoolean(false);
		/*
		 * Zmienna typu "AtomicBoolean" odpowiedzialna jest za zatrzymanie pracy programu.
		 */
		
		final int numberOfProducers = 1;
		final int numberOfConsumers = 2;
		final ExecutorService executor = Executors.newFixedThreadPool(numberOfProducers + numberOfConsumers);
		/*
		 * Ilosc producentow oraz konsumentow bedzie suma wszystkich watkow
		 */
		
		final BlockingQueue<Optional<Path>> blockingQueue = new LinkedBlockingDeque<>(2);
		/*
		 * Kolejka blokujaca dwuelementowa
		 */
		
		/*
		 * WAtki producenta
		 * ______________________________________________
		 * Nazwa threadName jest inicjowane przez nazwe watku
		 * Tworzymy liste plikow i umieszczamy je w kolejce
		 * Zmienna typu boolean pomaga zasygnalizowac koniec , rowniez pozostalym watkom
		 * Jestesmy na biezaco informowani odnosnie stanow watkow
		 */
		Runnable producer = () -> {
			String producerThreadName = Thread.currentThread().getName();
			System.out.println("Uwaga!Uruchomiono watek producenta: " + producerThreadName);
			fileAnalyzer(fileStorage, blockingQueue);
			teaTime.set(true);
			System.out.println("Uwaga. Zostal zakonczony watek producenta: " + producerThreadName);
		};

		/*
		 * Watki konsumenta
		 */
		Runnable consumer = () -> {
			String consumerThreadName = Thread.currentThread().getName();
			System.out.println("Uruchomiono watek konsumenta: " + consumerThreadName);
			while (true) {
				try {
					if (!blockingQueue.isEmpty()) {
						/*
						 *  Gdy kolejka nie jest pusta wykonuje sie dalsze dzialanie
						 *  Wywolujemy metode przeszukajaca i sortujaca plikik tekstowe
						 */
						Map<String, Long> wordAnalyzer = wordCounter(blockingQueue.take(), wordLimit);
						
						/*
						 * Gdy kolejka nie jest pusta zatrzymywania jest praca watkow
						 */
					} else { 
						if (teaTime.compareAndSet(true, false)) {
							teaTime.set(true);
							System.out.println("Zakonczono watek producenta: " + consumerThreadName);
							break;
						}
					}

				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		};

		/*
		 * uruchomienie watkow producenta a nastepnie konsumenta
		 */
		for (int i = 0; i < numberOfProducers; i++)
			executor.execute(producer);
		for (int i = 0; i < numberOfConsumers; i++)
			executor.execute(consumer);

		executor.shutdown(); //Inizjalizuje uporzadkowanie zamkniecie, nowe zadanie nie bedzie przyjmowane.

		try {
			TimeUnit.SECONDS.sleep(5);
		
		} catch (InterruptedException e) {
		}
		teaTime.set(true);


		System.out.println("Dzialanie programu zostalo zakonczone.");
		System.exit(0);

	}

	/*
	 * Metoda analziujaca dane tekstowe i ustawiajaca je w kolejce
	 * Tworzona jest lista plikow znajdujacych sie w katalogu, ktorego sciezka okreslona jest
	 * 		w zmiennej "fileStorage" przekazanej tutaj jako "directoryName". 
	 * 		W przypadku, gdy plik ma odpowiednie rozszerzenie, a kolejka nie jest pelna to dodawany jest do niej ten plik. 
	 * 		Proces dodawania do kolejki powtarza sie az do skutku (petla while).
	 */
	public static void fileAnalyzer(String directoryName, BlockingQueue<Optional<Path>> kolejka) {
		File fileDirectory = new File(directoryName);
		File[] fileList = fileDirectory.listFiles();
		boolean fullQueue = false;
		if (fileList != null)
			for (File file : fileList) {
				do {
					if (file.getName().toLowerCase().endsWith(".txt")) {
						Optional<Path> optPath = Optional.ofNullable(file.toPath());
						try {
							kolejka.add(optPath);
							fullQueue = false;
						} catch (IllegalStateException e) {
							fullQueue = true;
						}
					} 
				} while (fullQueue);
			}
	}

	private static Map<String, Long> wordCounter(Optional<Path> path, Long wordLimit) {
		String textLine;
		Path filePath = path.get();
		String textRead = "";
		Map<String, Long> map = new LinkedHashMap<>();
		try (BufferedReader fileReader = Files.newBufferedReader(filePath.toAbsolutePath(), StandardCharsets.UTF_8)) {
			while ((textLine = fileReader.readLine()) != null) {
				textRead += " " + textLine;
			}
			/*
			 * W ponizszym fragmencie kodu przygotowywana jest zawartosc czytanego pliku.
			 * Litery sa najpierw zamieniane na "male", 
			 * 		potem usuwane sa niepotrebdane znaki, 
			 * 		a nastepnie ciag znakow dzielony jest na poszczegolne wyrazy.
			 */
			String firstWordValidator = textRead.toLowerCase();
			String secondWordValidator = firstWordValidator.replaceAll("[^a-z]", " ");
			String[] words = secondWordValidator.split(" ", 0);
			for (String word : words) {
				if (word.length() > 2) {
					/*
					 * Instrukcja warunkowa sprawdzajaca czy analizowany wyraz jest dluzszy niz dwa znaki.
					 */
					if (map.containsKey(word)) {
						/*
						 * Instrukcja warunkowa zliczajcaa kolejne slowa, 
						 * 		jezeli zawieraja sie one w ustalonych kryteriach.
						 */
						map.put(word, map.get(word) + 1);
					} else {
						map.put(word, 1l);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		/*
		 * Ponizej posortowane zostanie 10 najczeciej wystepujacych wyrazow.
		 */
		Map<String, Long> mostUsedWordsSort = map.entrySet().stream()
				.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).limit(10).collect(Collectors.toMap(
						Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue, LinkedHashMap::new));


		System.out.println("Przeanalizowano plik: " + filePath + "\nStatystyka wyrazow nieposortowanych: " + map
				+ "\nStatystyka wyraz�w posortowanych: " + mostUsedWordsSort);
		/*
		 * Kod odpowiedzialny za wypisanie statystyki podsumowujacej prace  programu.
		 */
		
		
		return null;
	}
}
