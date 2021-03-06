import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/**
 * @author Bruno Tomé - 0011254
 * @author Cláudio Menezes - 0011255
 * @since 19/06/2016
 */
public class Table {
    private final Double alfa = 0.15;
    private int objectiveFunction;
    private ArrayList<Class> listClassNonAllocated = new ArrayList<>();
    private ArrayList<Class> listClassAllocated = new ArrayList<>();
    private int[][] table;
    private int[][] usedRooms; // Retorna quantidade de aulas da disciplina  na sala na semana. Verificar quantas salas estão ocupadas por disciplina
    private int[][] busyDays; // Retorna a quantidade de aulas da disciplina no dia. Contar em quantos dias há aulas de uma disciplina
    private int[][][] curriculaDaysPeriods; // Retorna a quantidade de aulas do currículo c alocadas no dia d e horário p
    private Problem currentProblem;

    public Table(Problem currentProblem) {
        this.currentProblem = currentProblem;
        this.table = new int[this.currentProblem.getnRooms()][this.currentProblem.getTotalSchedules()];
        this.busyDays = new int[this.currentProblem.getCourses().length][this.currentProblem.getnDays()];
        this.usedRooms = new int[this.currentProblem.getCourses().length][this.currentProblem.getnRooms()];
        this.curriculaDaysPeriods = new int[this.currentProblem.getCurriculas().length][this.currentProblem.getnDays()][this.currentProblem.getnPeriodsPerDay()];
        this.initializeBusyUsedMatrix();
        this.initializeCurriculaDaysPeriodsMatrix();
        this.generateInicialTable();
    }

    public int[][][] getCurriculaDaysPeriods() {
        return curriculaDaysPeriods;
    }

    public void setCurriculaDaysPeriods(int[][][] curriculaDaysPeriods) {
        this.curriculaDaysPeriods = curriculaDaysPeriods;
    }

    public int[][] getUsedRooms() {
        return usedRooms;
    }

    public void setUsedRooms(int[][] usedRooms) {
        this.usedRooms = usedRooms;
    }

    public int[][] getBusyDays() {
        return busyDays;
    }

    public void setBusyDays(int[][] busyDays) {
        this.busyDays = busyDays;
    }

    public int[][] getTable() {
        return table;
    }

    public void setTable(int[][] table) {
        this.table = table;
    }

    public Problem getCurrentProblem() {
        return currentProblem;
    }

    public void setCurrentProblem(Problem currentProblem) {
        this.currentProblem = currentProblem;
    }

    public int getObjectiveFunction() {
        return objectiveFunction;
    }

    public void setObjectiveFunction(int objectiveFunction) {
        this.objectiveFunction = objectiveFunction;
    }

    /**
     * Get the min and the max viable schedules
     *
     * @param c is a class
     * @return int[] is a range with min and max value of viable schedules
     */
    private int[] getMinMax(Class c) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (Integer[] s : c.getViableSchedules()) {
            if (s[2] < min) {
                min = s[2];
            }

            if (s[2] > max) {
                max = s[2];
            }
        }

        return new int[]{min, max};
    }

    /**
     * Cut off the list of viableSchedules based on cost to allocated be smaller than inteval
     *
     * @param c is a class
     * @return c another class with some viables schedules removed
     */
    public Class restrictedSchedulesList(Class c) {
        c = this.generateViableSchedules(c);

        while (c.getViableSchedules().isEmpty()) {
            this.generateNewSchedules(c);
        }

        int[] minMax = this.getMinMax(c);
        int interval = (int) (minMax[0] + (this.alfa * (minMax[1] - minMax[0])));

        ArrayList<Integer[]> aux = new ArrayList<>(c.getViableSchedules());
        for (Integer[] s : aux) {
            if (s[2] > interval) {
                c.getViableSchedules().remove(s);
            }
        }

        return c;
    }

    /**
     * Update all dynamic matrix after allocated a class in the table
     *
     * @param c    is a class
     * @param type for sum (true) or subtract (false) into matrix
     */
    public void refreshDynamicMatrix(Class c, Boolean type) {
        int course = this.currentProblem.getCourseFromInt(c.getIdxClass()).getIdx();
        int curricula = this.currentProblem.getCurriculaFromCourse(this.currentProblem.getCourseFromInt(c.getIdxClass())).getIdx();
        int day = Math.floorDiv(c.getViableSchedules().get(0)[1], this.currentProblem.getnPeriodsPerDay());
        int room = c.getViableSchedules().get(0)[0];
        int period = c.getViableSchedules().get(0)[1] % this.currentProblem.getnPeriodsPerDay();

        if (type) {
            this.busyDays[course][day] += 1;
            this.usedRooms[course][room] += 1;
            this.curriculaDaysPeriods[curricula][day][period] += 1;
        } else {
            this.busyDays[course][day] -= 1;
            this.usedRooms[course][room] -= 1;
            this.curriculaDaysPeriods[curricula][day][period] -= 1;
        }

    }

    /**
     * Generate a inicial table. That is the inicial solution
     */
    public void generateInicialTable() {
        this.fillTable();
        this.fillClassNonAllocated();

        int choosen;

        while (!this.listClassNonAllocated.isEmpty()) {
            Class classAux = this.listClassNonAllocated.get(0);
            classAux = this.restrictedSchedulesList(classAux);

            while (classAux.getViableSchedules().isEmpty()) {
                classAux = this.generateNewSchedules(classAux);
            }

            Random random = new Random();
            choosen = random.nextInt(classAux.getViableSchedules().size());
            int line = classAux.getViableSchedules().get(choosen)[0];
            int column = classAux.getViableSchedules().get(choosen)[1];
            int cost = classAux.getViableSchedules().get(choosen)[2];
            this.table[line][column] = classAux.getIdxClass();

            classAux.getViableSchedules().removeAll(classAux.getViableSchedules());
            classAux.getViableSchedules().add(new Integer[]{line, column, cost});

            this.listClassAllocated.add(classAux);
            this.listClassNonAllocated.remove(0);
            this.refreshDynamicMatrix(classAux, true);
        }
        this.calculateObjetiveFunction();
        System.out.println("All fucking classes allocated: " + this.getListClassAllocated().size());
        System.out.println("objective function:  " + this.objectiveFunction);
        System.out.println(this.toString());
    }

    /**
     * Initialize the usedRooms matrix and busyDays matrix with zeros
     */
    private void initializeBusyUsedMatrix() {
        for (int i = 0; i < this.currentProblem.getCourses().length; i++) {
            for (int j = 0; j < this.currentProblem.getnRooms(); j++) {
                this.usedRooms[i][j] = 0;
            }

            for (int j = 0; j < this.currentProblem.getnDays(); j++) {
                this.busyDays[i][j] = 0;
            }
        }
    }

    /**
     * Initialize the curriculaDaysPeriods matrix with zeros
     */
    private void initializeCurriculaDaysPeriodsMatrix() {
        for (int i = 0; i < this.currentProblem.getCurriculas().length; i++) {
            for (int j = 0; j < this.currentProblem.getnDays(); j++) {
                for (int k = 0; k < this.currentProblem.getnPeriodsPerDay(); k++) {
                    this.curriculaDaysPeriods[i][j][k] = 0;
                }
            }
        }
    }


    /**
     * Fill the array with the unavailable schedules array
     */
    private void fillClassNonAllocated() {
        // Run the matrix, and catch inviability of same teacher or same curricula
        for (int l = 0; l < this.currentProblem.getTotalClass(); l++) {
            Class currentClass = new Class();
            currentClass.setIdxClass(l);
            currentClass.setScheduleViability(this.currentProblem.getTotalSchedules());
            for (int c = 0; c < this.currentProblem.getTotalClass(); c++) {
                if (this.currentProblem.getClassClass()[l][c] == 1) {
                    currentClass.setScheduleViability(currentClass.getScheduleViability() - 1);
                }
            }

            // Run the matrix classSchedule, and catch inviability Schedules for the currentClass
            for (int k = 0; k < currentProblem.getTotalSchedules(); k++) {
                if (this.currentProblem.getClassSchedules()[l][k] == 1) {
                    currentClass.setScheduleViability(currentClass.getScheduleViability() - 1);
                }
            }

            this.listClassNonAllocated.add(currentClass);

        }
        Collections.sort(this.listClassNonAllocated, (c1, c2) -> Double.compare(c1.getScheduleViability(), c2.getScheduleViability()));
    }

    /**
     * When viableSchedules are empity we need to rerange the table
     *
     * @param c is a class for pass as a parameter to remove from viable schedules
     * @return
     */
    public Class generateNewSchedules(Class c) {
        Random random = new Random();
        int choosen = random.nextInt(this.listClassAllocated.size());
        Class classChoosen = this.listClassAllocated.get(choosen);
        this.listClassAllocated.remove(choosen);
        this.refreshDynamicMatrix(classChoosen, false);

        int line = classChoosen.getViableSchedules().get(0)[0];
        int column = classChoosen.getViableSchedules().get(0)[1];

        this.table[line][column] = -1;
        this.listClassNonAllocated.add(classChoosen);
        return this.generateViableSchedules(c);
    }

    /**
     * Verify isolated class in curricula (weak constraint)
     *
     * @param curr is a curricula
     * @return int is a the number of isolated classes
     */
    public int isolatedClassesPerCurricula(int curr) {
        int sumIsoletedClass = 0;
        for (int i = 0; i < this.currentProblem.getnDays(); i++) {
            for (int p = 0; p < this.currentProblem.getnPeriodsPerDay(); p++) {
                if ((p > 0) && (p < currentProblem.getnPeriodsPerDay() - 1)) {
                    if ((this.curriculaDaysPeriods[curr][i][p - 1] == 0) && (this.curriculaDaysPeriods[curr][i][p + 1] == 0)) {
                        sumIsoletedClass += 2;
                    } else if ((this.curriculaDaysPeriods[curr][i][p - 1] == 0) || (this.curriculaDaysPeriods[curr][i][p + 1] == 0)) {
                        sumIsoletedClass++;
                    }
                } else {
                    if ((p == 0) && (this.curriculaDaysPeriods[curr][i][p + 1] == 0)) {
                        sumIsoletedClass++;
                    }
                    if ((p == currentProblem.getnPeriodsPerDay()) && ((this.curriculaDaysPeriods[curr][i][p - 1] == 0))) {
                        sumIsoletedClass++;
                    }
                }
            }
        }

        return sumIsoletedClass;
    }

    /**
     * Verify how many rooms a course use
     *
     * @param course is a course to check how many rooms
     * @return int number of rooms
     */
    private int stabilityRoom(Course course) {
        int stability = 0;
        for (int i = 0; i < this.currentProblem.getnRooms(); i++) {
            if (this.usedRooms[course.getIdx()][i] > 0) {
                stability++;
            }
        }
        return stability;
    }


    /**
     * Verify how many days a course have
     *
     * @param course to check how many days it has
     * @return workDays is a int indicating the work days of a course
     */
    public int daysOfWork(Course course) {
        int workDays = 0;
        for (int i = 0; i < this.currentProblem.getnDays(); i++) {
            if (this.busyDays[course.getIdx()][i] > 0) {
                workDays++;
            }
        }
        return workDays;
    }


    public void calculateObjetiveFunction() {
        int cost = 0;

        // 1 - weak constraint: room capacity
        for (Course c : this.currentProblem.getCourses()) {
            int weak = c.getnStudents();
            for (int r = 0; r < this.currentProblem.getnRooms(); r++) {
                if (this.usedRooms[c.getIdx()][r] > 0) {
                    if (weak > this.currentProblem.getRoomCapacity(r)) {
                        cost += Math.abs(weak - this.currentProblem.getRoomCapacity(r));
                    }
                }
            }
            // 2 - weak constraint: min days necessity for a class
            weak = c.getMinClassDays();
            if (weak > this.daysOfWork(c)) {
                cost += Math.abs(((weak - daysOfWork(c)) * 5));
            }

            // 3 - weak constraint: all class in the same room
            weak = this.stabilityRoom(c);
            cost += Math.abs(weak);

            // 4 - weak constraint: isolateded classes
            Curricula curriculaAux = this.currentProblem.getCurriculaFromCourse(c);
            if (curriculaAux != null) {
                weak = this.isolatedClassesPerCurricula(curriculaAux.getIdx());
                cost += Math.abs((weak * 2));
            }
        }

        this.objectiveFunction = cost;
    }


    /**
     * Calculate the cost to allocated a class on the table, based on weak constraints
     *
     * @param c    is a class that will be tested
     * @param room is a room that will be tested
     * @return cost is the cost to alocate the class in this room
     */
    public int alocationClassCost(Class c, int room) {
        int cost = 0;
        Course caux;
        caux = this.currentProblem.getCourseFromInt(c.getIdxClass());

        // 1 - weak constraint: room capacity
        int weak = caux.getnStudents();
        if (weak > this.currentProblem.getRoomCapacity(room)) {
            cost = Math.abs(weak - this.currentProblem.getRoomCapacity(room));
        }
        // 2 - weak constraint: min days necessity for a class
        weak = caux.getMinClassDays();
        if (weak > this.daysOfWork(caux)) {
            cost += Math.abs((weak - daysOfWork(caux)) * 5);
        }
        // 3 - weak constraint: all class in the same room
        weak = this.stabilityRoom(caux);
        cost += Math.abs(weak);
        // 4 - weak constraint: isolateded classes
        Curricula curriculaAux = this.currentProblem.getCurriculaFromCourse(caux);
        if (curriculaAux != null) {
            weak = this.isolatedClassesPerCurricula(curriculaAux.getIdx());
            cost += Math.abs(weak * 2);
        }

        return cost;
    }

    /**
     * Generate a list of viable schedules for a class
     *
     * @param c is a class to generate viable schedules
     * @return Class c
     */
    public Class generateViableSchedules(Class c) {
        boolean flagSameCurricula;
        boolean flagSameClass;
        Integer[] viableSchedules = new Integer[3];

        for (int i = 0; i < this.currentProblem.getTotalSchedules(); i++) {
            flagSameCurricula = false;
            flagSameClass = false;
            if (this.currentProblem.getClassSchedules()[c.getIdxClass()][i] == 0) {
                for (int j = 0; j < this.currentProblem.getnRooms(); j++) {
                    if (this.table[j][i] != -1) {
                        if (this.currentProblem.courseSameCurricula(c.getIdxClass(), this.table[j][i])) {
                            flagSameCurricula = true;
                        }

                        if (this.currentProblem.sameCourse(c.getIdxClass(), this.table[j][i])) {
                            flagSameClass = true;
                        }
                    }
                }

                for (int j = 0; j < this.currentProblem.getnRooms(); j++) {
                    if ((this.table[j][i] == -1)) {
                        if ((flagSameClass == false) && (flagSameCurricula == false)) {
                            viableSchedules[0] = j;
                            viableSchedules[1] = i;
                            viableSchedules[2] = this.alocationClassCost(c, j);
                            c.getViableSchedules().add(viableSchedules);
                        }
                    }
                }
            }
        }

        return c;
    }

    /**
     * Fill table with -1
     */
    private void fillTable() {
        for (int room = 0; room < this.table.length; room++) {
            for (int schedule = 0; schedule < this.table[0].length; schedule++) {
                this.table[room][schedule] = -1;
            }
        }
    }

    public ArrayList<Class> getListClassAllocated() {
        return listClassAllocated;
    }

    public String toString() {
        String line = "        ";
        int qtdSpaceBefore = this.currentProblem.getnPeriodsPerDay() >= 5 ? 3 : 1;
        int qtdSpaceAfter;
        for (int i = 0; i < this.currentProblem.getnDays(); i++) {
            qtdSpaceAfter = i < 10 ? 2 : 4;
            for (int j = 0; j < (this.currentProblem.getnPeriodsPerDay() / 2) + qtdSpaceBefore; j++) {
                line += "\t";
            }
            line += "Day" + (i + 1);
            for (int j = 0; j < (this.currentProblem.getnPeriodsPerDay() / 2) + qtdSpaceAfter; j++) {
                line += "\t";
            }
            line += "||";
        }
        line += "\nSchedules  ";
        for (int i = 0; i < this.currentProblem.getnDays(); i++) {
            for (int j = 0; j < this.currentProblem.getnPeriodsPerDay(); j++) {
                line += "\t" + (j + 1) + "\t";
            }
        }

        for (int i = 0; i < this.currentProblem.getnRooms(); i++) {
            line += "\n\nRoom" + i + "\t";
            for (int j = 0; j < this.currentProblem.getTotalSchedules(); j++) {
                line += "\t" + this.table[i][j] + "\t";
            }
        }
        return line;
    }

}
