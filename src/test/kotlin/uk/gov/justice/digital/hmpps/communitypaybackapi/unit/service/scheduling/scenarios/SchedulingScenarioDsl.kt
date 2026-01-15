package uk.gov.justice.digital.hmpps.communitypaybackapi.unit.service.scheduling.scenarios

import org.assertj.core.api.Assertions.assertThat
import uk.gov.justice.digital.hmpps.communitypaybackapi.factory.random
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

@DslMarker
annotation class SchedulingScenarioDslMarker

@SchedulingScenarioDslMarker
class SchedulingScenarioBuilder {
  private var today: LocalDate = LocalDate.now()
  private var requirementLength: Duration? = null
  private val allocations = mutableListOf<SchedulingAllocation>()
  private val existingAppointments = mutableListOf<SchedulingExistingAppointment>()
  private val nonWorkingDates = mutableListOf<LocalDate>()
  private val projects = mutableMapOf<String, SchedulingProject>()
  private var scenarioId: String? = null

  fun scenarioId(scenarioId: String) {
    this.scenarioId = scenarioId
  }

  fun given(init: GivenContext.() -> Unit) {
    GivenContext().apply(init)
  }

  fun then(init: ThenContext.() -> Unit) {
    val request = SchedulingRequest.empty().copy(
      today = today,
      trigger = "Unit Test: $scenarioId",
      requirement = SchedulingRequirement(crn = "CRN1", deliusEventNumber = 5, requirementLengthMinutes = requireNotNull(requirementLength)),
      allocations = SchedulingAllocations(allocations),
      existingAppointments = SchedulingExistingAppointments(existingAppointments),
      nonWorkingDates = SchedulingNonWorkingDates(nonWorkingDates),
    )

    val result = Scheduler().producePlan(request)
    ThenContext(result, request).apply(init)
  }

  @SchedulingScenarioDslMarker
  inner class GivenContext {
    fun todayIs(dayOfWeek: DayOfWeek) {
      this@SchedulingScenarioBuilder.today = LocalDate.now().with(TemporalAdjusters.nextOrSame(dayOfWeek))
    }

    fun projectExistsWithCode(code: String, init: ProjectBuilder.() -> Unit = {}) {
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
          this@SchedulingScenarioBuilder.allocations,
        ),
      )
    }

    fun nonWorkingDate(offsetDays: Int) {
      this@SchedulingScenarioBuilder.nonWorkingDates.add(
        this@SchedulingScenarioBuilder.today.plusDays(offsetDays.toLong()),
      )
    }

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

    fun shouldCreateAppointments(toAddressShortfall: Duration? = null, init: ExpectedActionsBuilder.() -> Unit) {
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
      toAddressShortfall?.let {
        assertThat(insufficient.plan.shortfall).isEqualTo(it)
      }
    }

    fun noActionsExpected(toAddressShortfall: Duration? = null) {
      assertThat(result)
        .withFailMessage("Expected existing appointments to be insufficient")
        .isInstanceOf(SchedulerOutcome.ExistingAppointmentsInsufficient::class.java)

      val insufficient = result as SchedulerOutcome.ExistingAppointmentsInsufficient
      val actualCreations = insufficient.plan.actions.filterIsInstance<SchedulingAction.CreateAppointment>()
      assertThat(actualCreations).isEmpty()

      toAddressShortfall?.let {
        assertThat(insufficient.plan.shortfall).isEqualTo(it)
      }
    }
  }
}

class ProjectBuilder(private val code: String) {

  fun build(): SchedulingProject = SchedulingProject.valid().copy(code = code)
}

class AllocationBuilder {
  private var projectCode: String? = null
  private var frequency: SchedulingFrequency = SchedulingFrequency.WEEKLY
  private var dayOfWeek: DayOfWeek = DayOfWeek.MONDAY
  private var startTime: LocalTime? = null
  private var endTime: LocalTime? = null
  private var startDate: Int? = null
  private var endDate: Int? = null
  private var alias: String? = null

  fun alias(alias: String) {
    this.alias = alias
  }
  fun projectCode(code: String) {
    projectCode = code
  }
  fun frequency(schedulingFrequency: SchedulingFrequency) {
    frequency = schedulingFrequency
  }
  fun onWeekDay(day: DayOfWeek) {
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
  fun startingInDays(days: Int) {
    startDate = days
  }
  fun endingInDays(days: Int) {
    endDate = days
  }

  fun build(today: LocalDate, projects: Map<String, SchedulingProject>): SchedulingAllocation {
    val project = projects[projectCode] ?: error("Project $projectCode not found")
    return SchedulingAllocation(
      id = alias.hashCode().toLong(),
      alias = alias,
      project = project,
      frequency = frequency,
      dayOfWeek = dayOfWeek,
      startDateInclusive = startDate?.let { today.plusDays(it.toLong()) } ?: today,
      endDateInclusive = endDate?.let { today.plusDays(it.toLong()) },
      startTime = startTime ?: error("Start time must be specified for allocation $alias"),
      endTime = endTime ?: error("End time must be specified for allocation $alias"),
    )
  }
}

class AppointmentBuilder {
  private var projectCode: String? = null
  private var allocationAlias: String? = null
  private var date: Int = 0
  private var startTime: LocalTime? = null
  private var endTime: LocalTime? = null
  private var hasOutcome: Boolean = false
  private var creditedTime: Duration? = null

  fun projectCode(code: String) {
    projectCode = code
  }
  fun allocation(id: String) {
    allocationAlias = id
  }
  fun manual() {
    allocationAlias = null
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

  fun build(today: LocalDate, allocations: List<SchedulingAllocation>): SchedulingExistingAppointment {
    val allocation = allocationAlias?.let { id ->
      allocations.find { it.id == id.hashCode().toLong() }
    }

    return SchedulingExistingAppointment(
      id = Long.random(),
      projectCode = projectCode ?: error("Project code must be specified for appointment"),
      date = today.plusDays(date.toLong()),
      startTime = startTime ?: error("Start time must be specified for appointment"),
      endTime = endTime ?: error("End time must be specified for appointment"),
      hasOutcome = hasOutcome,
      minutesCredited = creditedTime,
      allocationId = allocation?.id,
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
  private var projectCode: String? = null
  private var allocationAlias: String? = null
  private var offsetDays: Int = 0
  private var startTime: LocalTime? = null
  private var endTime: LocalTime? = null

  fun projectCode(code: String) {
    projectCode = code
  }
  fun allocation(alias: String) {
    allocationAlias = alias
  }
  fun todayWithOffsetDays(offsetDays: Int) {
    this@ExpectedAppointmentBuilder.offsetDays = offsetDays
  }
  fun todayWithOffsetDays() {
    offsetDays = 0
  }
  fun from(time: String) {
    startTime = LocalTime.parse(time)
  }
  fun until(time: String) {
    endTime = LocalTime.parse(time)
  }

  fun build(today: LocalDate, projects: Map<String, SchedulingProject>, allocations: List<SchedulingAllocation>): SchedulingAction.CreateAppointment {
    val project = projects[projectCode] ?: error("Project $projectCode not found")
    val allocation = allocations.find { it.id == allocationAlias.hashCode().toLong() }
      ?: error("Allocation $allocationAlias not found")

    return SchedulingAction.CreateAppointment(
      toCreate = SchedulingRequiredAppointment(
        date = today.plusDays(offsetDays.toLong()),
        startTime = startTime ?: error("Start time must be specified for appointment"),
        endTime = endTime ?: error("End time must be specified for appointment"),
        project = project,
        allocation = allocation,
      ),
    )
  }
}

fun schedulingScenario(init: SchedulingScenarioBuilder.() -> Unit) {
  SchedulingScenarioBuilder().apply(init)
}
