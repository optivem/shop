// Package log provides colored logging helpers.
package log

import (
	"fmt"
	"os"
)

const (
	cyan   = "\033[0;36m"
	green  = "\033[0;32m"
	yellow = "\033[0;33m"
	red    = "\033[0;31m"
	nc     = "\033[0m"
)

func Log(msg string) {
	fmt.Printf("%s>%s %s\n", cyan, nc, msg)
}

func Logf(format string, args ...any) {
	Log(fmt.Sprintf(format, args...))
}

func OK(msg string) {
	fmt.Printf("%sOK%s %s\n", green, nc, msg)
}

func OKf(format string, args ...any) {
	OK(fmt.Sprintf(format, args...))
}

func Warn(msg string) {
	fmt.Printf("%sWARN%s %s\n", yellow, nc, msg)
}

func Warnf(format string, args ...any) {
	Warn(fmt.Sprintf(format, args...))
}

func Fail(msg string) {
	fmt.Printf("%sFAIL%s %s\n", red, nc, msg)
}

func Failf(format string, args ...any) {
	Fail(fmt.Sprintf(format, args...))
}

// StepError is a sentinel type used by Fatal to allow the step runner to catch failures.
type StepError struct {
	Msg string
}

func (e *StepError) Error() string { return e.Msg }

// Fatal prints an error and panics with a StepError (caught by the step runner).
// For use during step execution. For pre-validation failures, use FatalExit.
func Fatal(msg string) {
	fmt.Fprintf(os.Stderr, "%sFATAL:%s %s\n", red, nc, msg)
	panic(&StepError{Msg: msg})
}

func Fatalf(format string, args ...any) {
	Fatal(fmt.Sprintf(format, args...))
}

// FatalExit prints an error and exits immediately. Use for pre-validation failures only.
func FatalExit(msg string) {
	fmt.Fprintf(os.Stderr, "%sFATAL:%s %s\n", red, nc, msg)
	os.Exit(1)
}
