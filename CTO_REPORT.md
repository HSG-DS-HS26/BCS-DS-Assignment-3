# Report to the CTO

Answer below each question after Tasks 4 and 5 of [TASKS.md](TASKS.md). For every number, say where you found it.

## Task 4: ServerErrors

1. Copy this table into your answer and fill it in for the original and for your version:

   | | Original | Your version |
   |---|---|---|
   | Number of stages | | |
   | Shuffle Write per stage (size / records) | | |
   | Duration per stage | | |
   | Duration of the application | | |
   | Shuffle operations | | |

   **Answer:**

2. For each shuffle in the original version: which line of code causes it, what are the key and the value of the records it sends to other workers, and how many records does it send?

   **Answer:**

3. For each change you made to the code: which numbers in the table of question 1 did it affect, and why did they change?

   **Answer:**

## Task 5: HDFS

4. Why is every block of `/data/join/orders.csv` stored on more than a single DataNode?

   **Answer:**

5. How did Spark decide which worker reads which block of `orders.csv` when you ran the join from HDFS?

   **Answer:**

6. Why could Spark repeat only some stages in the run with `--problematic-system` instead of starting the job again?

   **Answer:**

7. How did HDFS recover from the crash of the DataNode? Use what you observed on the Datanodes page of the NameNode UI.

   **Answer:**
