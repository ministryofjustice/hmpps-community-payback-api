package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.scheduling.scenarios

import org.assertj.core.api.Assertions.assertThat
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.scheduling.empty
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.scheduling.valid
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingAction
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingAllocation
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingAllocations
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingExistingAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingExistingAppointments
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingFrequency
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingNonWorkingDates
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingProject
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingRequest
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingRequiredAppointment
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.SchedulingRequirement
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal.Scheduler
import uk.gov.justice.digital.hmpps.communitypaybackapi.service.scheduling.internal.Scheduler.SchedulerOutcome
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import java.util.UUID

@DslMarker
annotation class SchedulingScenarioDslMarker

@SchedulingScenarioDslMarker
class SchedulingScenarioBuilder {
  private var today: LocalDate = LocalDate.now()
  private var requirementLength: Duration = Duration.ZERO
  private val allocations = mutableListOf<SchedulingAllocation>()
  private val existingAppointments = mutableListOf<SchedulingExistingAppointment>()
  private val nonWorkingDates = mutableListOf<LocalDate>()
  private val projects = mutableMapOf<String, SchedulingProject>()
  private var testName: String? = null

  fun test(testName: String) {
    this.testName = testName
  }

  fun given(init: GivenContext.() -> Unit) {
    GivenContext().apply(init)
  }

  fun whenScheduling(init: WhenContext.() -> Unit) {
    WhenContext().apply(init)
  }

  fun then(init: ThenContext.() -> Unit) {
    val request = SchedulingRequest.empty().copy(
      today = today,
      trigger = "Unit Test: $testName",
      requirement = SchedulingRequirement(requirementLength),
      allocations = SchedulingAllocations(allocations),
      existingAppointments = SchedulingExistingAppointments(existingAppointments),
      nonWorkingDates = SchedulingNonWorkingDates(nonWorkingDates),
    )

    val result = Scheduler.producePlan(request)
    ThenContext(result, request).apply(init)
  }

  @SchedulingScenarioDslMarker
  inner class GivenContext {
    fun today(dayOfWeek: DayOfWeek) {
      this@SchedulingScenarioBuilder.today = LocalDate.now().with(TemporalAdjusters.nextOrSame(dayOfWeek))
    }

    fun project(code: String, init: ProjectBuilder.() -> Unit = {}) {
      val builder = ProjectBuilder(code)
      builder.apply(init)
      this@SchedulingScenarioBuilder.projects[code] = builder.build()
    }

    fun allocation(init: AllocationBuilder.() -> Unit) {
      val builder = AllocationBuilder()
      builder.apply(init)
      this@SchedulingScenarioBuilder.allocations.add(
        builder.build(this@SchedulingScenarioBuilder.today, this@SchedulingScenarioBuilder.projects),
      )
    }

    fun appointment(init: AppointmentBuilder.() -> Unit) {
      val builder = AppointmentBuilder()
      builder.apply(init)
      this@SchedulingScenarioBuilder.existingAppointments.add(
        builder.build(
          this@SchedulingScenarioBuilder.today,
          this@SchedulingScenarioBuilder.projects,
          this@SchedulingScenarioBuilder.allocations,
        ),
      )
    }

    fun nonWorkingDate(offsetDays: Int) {
      this@SchedulingScenarioBuilder.nonWorkingDates.add(
        this@SchedulingScenarioBuilder.today.plusDays(offsetDays.toLong()),
      )
    }
  }

  @SchedulingScenarioDslMarker
  inner class WhenContext {
    fun requirementIs(duration: Duration) {
      this@SchedulingScenarioBuilder.requirementLength = duration
    }

    fun requirementIsHours(hours: Long) {
      this@SchedulingScenarioBuilder.requirementLength = Duration.ofHours(hours)
    }
  }

  @SchedulingScenarioDslMarker
  inner class ThenContext(
    private val result: SchedulerOutcome,
    private val request: SchedulingRequest,
  ) {
    fun requirementAlreadySatisfied() {
      assertThat(result)
        .withFailMessage("Expected requirement to be already satisfied")
        .isInstanceOf(SchedulerOutcome.RequirementAlreadySatisfied::class.java)
    }

    fun existingAppointmentsSufficient() {
      assertThat(result)
        .withFailMessage("Expected existing appointments to be sufficient")
        .isInstanceOf(SchedulerOutcome.ExistingAppointmentsSufficient::class.java)
    }

    fun shouldCreateAppointments(init: ExpectedActionsBuilder.() -> Unit) {
      assertThat(result)
        .withFailMessage("Expected existing appointments to be insufficient")
        .isInstanceOf(SchedulerOutcome.ExistingAppointmentsInsufficient::class.java)

      val insufficient = result as SchedulerOutcome.ExistingAppointmentsInsufficient
      val expectedActions = ExpectedActionsBuilder(
        this@SchedulingScenarioBuilder.today,
        this@SchedulingScenarioBuilder.projects,
        request.allocations.allocations,
      )
      expectedActions.apply(init)

      val actualCreations = insufficient.plan.actions.filterIsInstance<SchedulingAction.CreateAppointment>()
      assertThat(actualCreations).containsExactlyInAnyOrderElementsOf(expectedActions.actions)
    }

    fun withShortfall(duration: Duration) {
      assertThat(result).isInstanceOf(SchedulerOutcome.ExistingAppointmentsInsufficient::class.java)
      val insufficient = result as SchedulerOutcome.ExistingAppointmentsInsufficient
      assertThat(insufficient.plan.shortfall).isEqualTo(duration)
    }
  }
}

class ProjectBuilder(private val code: String) {

  fun build(): SchedulingProject = SchedulingProject.valid().copy(code = code)
}

class AllocationBuilder {
  private var id: String = ""
  private var projectCode: String = ""
  private var frequency: SchedulingFrequency = SchedulingFrequency.WEEKLY
  private var dayOfWeek: DayOfWeek = DayOfWeek.MONDAY
  private var startTime: LocalTime = LocalTime.of(10, 0)
  private var endTime: LocalTime = LocalTime.of(18, 0)
  private var startDate: Int? = null
  private var endDate: Int? = null
  private var alias: String? = null

  fun id(value: String) {
    id = value
  }
  fun project(code: String) {
    projectCode = code
  }
  fun frequency(schedulingFrequency: SchedulingFrequency) {
    frequency = schedulingFrequency
  }
  fun on(day: DayOfWeek) {
    dayOfWeek = day
  }
  fun from(time: String) {
    startTime = LocalTime.parse(time)
  }
  fun until(time: String) {
    endTime = LocalTime.parse(time)
  }
  fun startingToday() {
    startDate = 0
  }
  fun startingIn(days: Int) {
    startDate = days
  }
  fun endingIn(days: Int) {
    endDate = days
  }

  fun build(today: LocalDate, projects: Map<String, SchedulingProject>): SchedulingAllocation {
    val project = projects[projectCode] ?: error("Project $projectCode not found")
    return SchedulingAllocation(
      id = id.hashCode().toLong(),
      alias = alias,
      project = project,
      frequency = frequency,
      dayOfWeek = dayOfWeek,
      startDateInclusive = startDate?.let { today.plusDays(it.toLong()) } ?: today,
      endDateInclusive = endDate?.let { today.plusDays(it.toLong()) },
      startTime = startTime,
      endTime = endTime,
    )
  }
}

class AppointmentBuilder {
  private var projectCode: String = ""
  private var allocationId: String? = null
  private var date: Int = 0
  private var startTime: LocalTime = LocalTime.of(10, 0)
  private var endTime: LocalTime = LocalTime.of(18, 0)
  private var hasOutcome: Boolean = false
  private var creditedTime: Duration? = null

  fun project(code: String) {
    projectCode = code
  }
  fun allocation(id: String) {
    allocationId = id
  }
  fun manual() {
    allocationId = null
  }
  fun today(offsetDays: Int) {
    date = offsetDays
  }
  fun today() {
    date = 0
  }
  fun from(time: String) {
    startTime = LocalTime.parse(time)
  }
  fun until(time: String) {
    endTime = LocalTime.parse(time)
  }
  fun pending() {
    hasOutcome = false
    creditedTime = null
  }
  fun credited(duration: Duration) {
    hasOutcome = true
    creditedTime = duration
  }
  fun nonAttended() {
    hasOutcome = true
    creditedTime = null
  }

  fun build(today: LocalDate, projects: Map<String, SchedulingProject>, allocations: List<SchedulingAllocation>): SchedulingExistingAppointment {
    val project = projects[projectCode] ?: error("Project $projectCode not found")
    val allocation = allocationId?.let { id ->
      allocations.find { it.id == id.hashCode().toLong() }
    }

    return SchedulingExistingAppointment(
      id = UUID.randomUUID(),
      project = project,
      date = today.plusDays(date.toLong()),
      startTime = startTime,
      endTime = endTime,
      hasOutcome = hasOutcome,
      timeCredited = creditedTime,
      allocation = allocation,
    )
  }
}

class ExpectedActionsBuilder(
  private val today: LocalDate,
  private val projects: Map<String, SchedulingProject>,
  private val allocations: List<SchedulingAllocation>,
) {
  val actions = mutableListOf<SchedulingAction.CreateAppointment>()

  fun appointment(init: ExpectedAppointmentBuilder.() -> Unit) {
    val builder = ExpectedAppointmentBuilder()
    builder.apply(init)
    actions.add(builder.build(today, projects, allocations))
  }
}

class ExpectedAppointmentBuilder {
  private var projectCode: String = ""
  private var allocationId: String = ""
  private var date: Int = 0
  private var startTime: LocalTime = LocalTime.of(10, 0)
  private var endTime: LocalTime = LocalTime.of(18, 0)

  fun project(code: String) {
    projectCode = code
  }
  fun allocation(id: String) {
    allocationId = id
  }
  fun today(offsetDays: Int) {
    date = offsetDays
  }
  fun today() {
    date = 0
  }
  fun from(time: String) {
    startTime = LocalTime.parse(time)
  }
  fun until(time: String) {
    endTime = LocalTime.parse(time)
  }

  fun build(today: LocalDate, projects: Map<String, SchedulingProject>, allocations: List<SchedulingAllocation>): SchedulingAction.CreateAppointment {
    val project = projects[projectCode] ?: error("Project $projectCode not found")
    val allocation = allocations.find { it.id == allocationId.hashCode().toLong() }
      ?: error("Allocation $allocationId not found")

    return SchedulingAction.CreateAppointment(
      toCreate = SchedulingRequiredAppointment(
        date = today.plusDays(date.toLong()),
        startTime = startTime,
        endTime = endTime,
        project = project,
        allocation = allocation,
      ),
    )
  }
}

fun schedulingScenario(init: SchedulingScenarioBuilder.() -> Unit) {
  SchedulingScenarioBuilder().apply(init)
}
