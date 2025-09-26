package util

import (
	"bytes"
	"encoding/binary"
	"fmt"
	"os/exec"
	"runtime"
	"strings"

	"github.com/StackExchange/wmi"
)

type GetDeviceInfo struct {
	cpuID string
}

func NewGetDeviceInfo() *GetDeviceInfo {
	return &GetDeviceInfo{}
}

// 根据操作系统获取CPU序列号
func chooseNGet() (string, error) {
	switch runtime.GOOS {
	case "windows":
		return getWindowsCPUID()
	case "linux":
		return getLinuxCPUID()
	default:
		return "", fmt.Errorf("unsupported operating system: %s", runtime.GOOS)
	}
}

// 获取Windows系统的CPU ID
func getWindowsCPUID() (string, error) {

	// 使用WMI获取CPU ID（最快）
	cpuID, err := getCPUIDWMI()
	if err == nil && cpuID != "" {
		fmt.Println("Got CPU ID using WMI")
		return cpuID, nil
	}

	// 使用wmic命令（旧版Windows或启用了WMIC的系统）
	cpuID, err = getWindowsCPUIDFromWMIC()
	if err == nil && cpuID != "" {
		fmt.Println("Got CPU ID using WMIC")
		return cpuID, nil
	}

	// 使用PowerShell (Windows 7+都支持，但慢)
	cpuID, err = getWindowsCPUIDFromPowerShell()
	if err == nil && cpuID != "" {
		fmt.Println("Got CPU ID using PowerShell")
		return cpuID, nil
	}

	// 使用注册表 (获取到的并不是真正的CPU ID，但也能作为标识，作为最后手段)
	// 获取到的如：AMD64 Family 25 Model 117 Stepping 2-AMD Ryzen 7 8745H w/ Radeon 780M Graphics
	cpuID, err = getWindowsCPUIDFromRegistry()
	if err == nil && cpuID != "" {
		fmt.Println("Got CPU ID using Registry")
		return cpuID, nil
	}

	return "", fmt.Errorf("failed to get CPU ID from all available methods")
}

// 使用WMI获取CPU ID
type Win32_Processor struct {
    ProcessorId string
}
func getCPUIDWMI() (string, error) {
    var dst []Win32_Processor
    err := wmi.Query("SELECT ProcessorId FROM Win32_Processor", &dst)
    if err != nil {
        return "", err
    }
    if len(dst) > 0 {
        return dst[0].ProcessorId, nil
    }
    return "", fmt.Errorf("no processor info found")
}

// 使用PowerShell获取CPU ID
func getWindowsCPUIDFromPowerShell() (string, error) {
	// PowerShell命令，使用Get-WmiObject (兼容性更好)
	cmd := exec.Command("powershell", "-Command", 
		"Get-WmiObject -Class Win32_Processor | Select-Object -ExpandProperty ProcessorId")
	
	var out bytes.Buffer
	var stderr bytes.Buffer
	cmd.Stdout = &out
	cmd.Stderr = &stderr
	
	err := cmd.Run()
	if err != nil {
		// 尝试使用Get-CimInstance (Windows 8+)
		cmd = exec.Command("powershell", "-Command",
			"Get-CimInstance -ClassName Win32_Processor | Select-Object -ExpandProperty ProcessorId")
		
		out.Reset()
		stderr.Reset()
		cmd.Stdout = &out
		cmd.Stderr = &stderr
		
		err = cmd.Run()
		if err != nil {
			return "", fmt.Errorf("failed to execute PowerShell command: %v", err)
		}
	}
	cpuID := strings.TrimSpace(out.String())
	if cpuID == "" {
		return "", fmt.Errorf("empty CPU ID from PowerShell")
	}
	
	return cpuID, nil
}

// 使用WMIC命令获取CPU ID
func getWindowsCPUIDFromWMIC() (string, error) {
	cmd := exec.Command("wmic", "cpu", "get", "ProcessorId", "/value")
	
	var out bytes.Buffer
	var stderr bytes.Buffer
	cmd.Stdout = &out
	cmd.Stderr = &stderr
	
	err := cmd.Run()
	if err != nil {
		return "", fmt.Errorf("failed to execute wmic command: %v", err)
	}
	
	output := out.String()
	lines := strings.Split(output, "\n")
	for _, line := range lines {
		line = strings.TrimSpace(line)
		if strings.HasPrefix(line, "ProcessorId=") {
			cpuID := strings.TrimPrefix(line, "ProcessorId=")
			cpuID = strings.TrimSpace(cpuID)
			if cpuID != "" {
				return cpuID, nil
			}
		}
	}
	
	return "", fmt.Errorf("CPU ID not found in wmic output")
}

// 从注册表读取CPU信息
func getWindowsCPUIDFromRegistry() (string, error) {
	// 使用reg query命令读取注册表
	cmd := exec.Command("reg", "query", 
		"HKLM\\HARDWARE\\DESCRIPTION\\System\\CentralProcessor\\0", 
		"/v", "ProcessorNameString")
	
	var out bytes.Buffer
	var stderr bytes.Buffer
	cmd.Stdout = &out
	cmd.Stderr = &stderr
	
	err := cmd.Run()
	if err != nil {
		return "", fmt.Errorf("failed to query registry: %v", err)
	}
	
	// 获取处理器名称
	output := out.String()
	lines := strings.Split(output, "\n")
	processorName := ""
	
	for _, line := range lines {
		if strings.Contains(line, "ProcessorNameString") {
			parts := strings.Split(line, "    REG_SZ    ")
			if len(parts) >= 2 {
				processorName = strings.TrimSpace(parts[1])
				break
			}
		}
	}
	
	// 获取Identifier
	cmd = exec.Command("reg", "query",
		"HKLM\\HARDWARE\\DESCRIPTION\\System\\CentralProcessor\\0",
		"/v", "Identifier")
	
	out.Reset()
	stderr.Reset()
	cmd.Stdout = &out
	cmd.Stderr = &stderr
	
	err = cmd.Run()
	if err == nil {
		output = out.String()
		lines = strings.Split(output, "\n")
		
		for _, line := range lines {
			if strings.Contains(line, "Identifier") {
				parts := strings.Split(line, "    REG_SZ    ")
				if len(parts) >= 2 {
					identifier := strings.TrimSpace(parts[1])
					// 组合处理器名称和标识符作为唯一ID
					if processorName != "" && identifier != "" {
						return fmt.Sprintf("%s-%s", identifier, processorName), nil
					} else if identifier != "" {
						return identifier, nil
					}
				}
			}
		}
	}
	
	if processorName != "" {
		return processorName, nil
	}
	
	return "", fmt.Errorf("CPU info not found in registry")
}

// 获取Linux系统的CPU序列号
func getLinuxCPUID() (string, error) {
	// 使用原生CPUID指令
	cpuID, err := getLinuxCPUIDNative()
	if err == nil && cpuID != "" {
		fmt.Println("Got CPU ID using native CPUID instruction")
		return cpuID, nil
	}

	// 尝试使用dmidecode（需要root权限）
	cpuID, err = getLinuxCPUIDFromDmidecode()
	if err == nil && cpuID != "" {
		fmt.Println("Got CPU ID using dmidecode")
		return cpuID, nil
	}
	
	// 从/proc/cpuinfo读取
	cpuID, err = getLinuxCPUIDFromProcCPUInfo()
	if err == nil && cpuID != "" {
		fmt.Println("Got CPU ID using /proc/cpuinfo")
		return cpuID, nil
	}
	
	// 使用lscpu命令
	cpuID, err = getLinuxCPUIDFromLscpu()
	if err == nil && cpuID != "" {
		fmt.Println("Got CPU ID using lscpu")
		return cpuID, nil
	}
	
	return "", fmt.Errorf("failed to get CPU ID from all available methods")
}

// 使用CPUID指令获取CPU信息（Linux版本）
func getLinuxCPUIDNative() (string, error) {
	// 读取/proc/cpuinfo获取基本信息
	cmd := exec.Command("cat", "/proc/cpuinfo")
	var out bytes.Buffer
	cmd.Stdout = &out
	
	err := cmd.Run()
	if err != nil {
		return "", err
	}
	
	output := out.String()
	lines := strings.Split(output, "\n")
	
	var vendor, family, model, stepping, flags string
	for _, line := range lines {
		line = strings.TrimSpace(line)
		if strings.HasPrefix(line, "vendor_id") && vendor == "" {
			parts := strings.Split(line, ":")
			if len(parts) >= 2 {
				vendor = strings.TrimSpace(parts[1])
			}
		} else if strings.HasPrefix(line, "cpu family") && family == "" {
			parts := strings.Split(line, ":")
			if len(parts) >= 2 {
				family = strings.TrimSpace(parts[1])
			}
		} else if strings.HasPrefix(line, "model") && !strings.HasPrefix(line, "model name") && model == "" {
			parts := strings.Split(line, ":")
			if len(parts) >= 2 {
				model = strings.TrimSpace(parts[1])
			}
		} else if strings.HasPrefix(line, "stepping") && stepping == "" {
			parts := strings.Split(line, ":")
			if len(parts) >= 2 {
				stepping = strings.TrimSpace(parts[1])
			}
		} else if strings.HasPrefix(line, "flags") && flags == "" {
			parts := strings.Split(line, ":")
			if len(parts) >= 2 {
				flagList := strings.Fields(parts[1])
				// 取前几个重要的flags作为特征
				if len(flagList) > 5 {
					flags = strings.Join(flagList[:5], "-")
				}
			}
		}
	}
	
	if vendor != "" && family != "" && model != "" && stepping != "" {
		// 组合这些信息作为CPU标识，包含一些flags增加唯一性
		cpuID := fmt.Sprintf("%s-%s-%s-%s-%s", vendor, family, model, stepping, flags)
		return cpuID, nil
	}
	
	return "", fmt.Errorf("incomplete CPU info from native method")
}

// 使用dmidecode命令获取CPU ID（需要root权限）
func getLinuxCPUIDFromDmidecode() (string, error) {
	cmd := exec.Command("sudo", "dmidecode", "-t", "processor")
	
	var out bytes.Buffer
	var stderr bytes.Buffer
	cmd.Stdout = &out
	cmd.Stderr = &stderr
	
	err := cmd.Run()
	if err != nil {
		return "", err
	}
	
	output := out.String()
	lines := strings.Split(output, "\n")
	for _, line := range lines {
		line = strings.TrimSpace(line)
		if strings.Contains(line, "ID:") || strings.Contains(line, "ID ") {
			parts := strings.Split(line, ":")
			if len(parts) >= 2 {
				cpuID := strings.TrimSpace(parts[1])
				cpuID = strings.ReplaceAll(cpuID, " ", "")
				if cpuID != "" {
					return cpuID, nil
				}
			}
		}
	}
	
	return "", fmt.Errorf("CPU ID not found in dmidecode output")
}

// 从/proc/cpuinfo读取CPU信息
func getLinuxCPUIDFromProcCPUInfo() (string, error) {
	cmd := exec.Command("cat", "/proc/cpuinfo")
	
	var out bytes.Buffer
	var stderr bytes.Buffer
	cmd.Stdout = &out
	cmd.Stderr = &stderr
	
	err := cmd.Run()
	if err != nil {
		return "", fmt.Errorf("failed to read /proc/cpuinfo: %v", err)
	}
	
	output := out.String()
	lines := strings.Split(output, "\n")
	
	var vendor, family, model, stepping string
	for _, line := range lines {
		line = strings.TrimSpace(line)
		if strings.HasPrefix(line, "vendor_id") {
			parts := strings.Split(line, ":")
			if len(parts) >= 2 {
				vendor = strings.TrimSpace(parts[1])
			}
		} else if strings.HasPrefix(line, "cpu family") {
			parts := strings.Split(line, ":")
			if len(parts) >= 2 {
				family = strings.TrimSpace(parts[1])
			}
		} else if strings.HasPrefix(line, "model") && !strings.HasPrefix(line, "model name") {
			parts := strings.Split(line, ":")
			if len(parts) >= 2 {
				model = strings.TrimSpace(parts[1])
			}
		} else if strings.HasPrefix(line, "stepping") {
			parts := strings.Split(line, ":")
			if len(parts) >= 2 {
				stepping = strings.TrimSpace(parts[1])
			}
		}
		
		if vendor != "" && family != "" && model != "" && stepping != "" {
			break
		}
	}
	
	if vendor != "" || family != "" || model != "" || stepping != "" {
		cpuID := fmt.Sprintf("%s-%s-%s-%s", vendor, family, model, stepping)
		return cpuID, nil
	}
	
	return "", fmt.Errorf("CPU info not found in /proc/cpuinfo")
}

// 使用lscpu命令获取CPU信息
func getLinuxCPUIDFromLscpu() (string, error) {
	cmd := exec.Command("lscpu")
	
	var out bytes.Buffer
	var stderr bytes.Buffer
	cmd.Stdout = &out
	cmd.Stderr = &stderr
	
	err := cmd.Run()
	if err != nil {
		return "", fmt.Errorf("failed to execute lscpu: %v", err)
	}
	
	output := out.String()
	lines := strings.Split(output, "\n")
	
	var vendor, family, model, stepping string
	for _, line := range lines {
		line = strings.TrimSpace(line)
		if strings.HasPrefix(line, "Vendor ID:") {
			parts := strings.Split(line, ":")
			if len(parts) >= 2 {
				vendor = strings.TrimSpace(parts[1])
			}
		} else if strings.HasPrefix(line, "CPU family:") {
			parts := strings.Split(line, ":")
			if len(parts) >= 2 {
				family = strings.TrimSpace(parts[1])
			}
		} else if strings.HasPrefix(line, "Model:") {
			parts := strings.Split(line, ":")
			if len(parts) >= 2 {
				model = strings.TrimSpace(parts[1])
			}
		} else if strings.HasPrefix(line, "Stepping:") {
			parts := strings.Split(line, ":")
			if len(parts) >= 2 {
				stepping = strings.TrimSpace(parts[1])
			}
		}
	}
	
	if vendor != "" || family != "" || model != "" || stepping != "" {
		cpuID := fmt.Sprintf("%s-%s-%s-%s", vendor, family, model, stepping)
		return cpuID, nil
	}
	
	return "", fmt.Errorf("CPU info not found in lscpu output")
}

// 对外暴露获取CPU ID的函数
func (g *GetDeviceInfo) GetCPUID() (string, error) {
	fmt.Printf("Operating System: %s\n", runtime.GOOS)
	fmt.Printf("Architecture: %s\n", runtime.GOARCH)
	fmt.Println("----------------------------")
	
	cpuID, err := chooseNGet()
	if err != nil {
		fmt.Printf("Error getting CPU ID: %v\n", err)
		return "", err
	}
	
	fmt.Printf("CPU ID: %s\n", cpuID)
	g.cpuID = cpuID
	return cpuID, nil
}

// 处理字节序的辅助函数
func uint32ToBytes(n uint32) []byte {
	b := make([]byte, 4)
	binary.LittleEndian.PutUint32(b, n)
	return b
}

func bytesToString(b []byte) string {
	return strings.TrimRight(string(b), "\x00")
}